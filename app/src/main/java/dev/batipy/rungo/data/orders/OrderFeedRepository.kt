package dev.batipy.rungo.data.orders

import dev.batipy.rungo.data.auth.AuthRepository
import dev.batipy.rungo.data.auth.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

private const val WS_BASE_URL = "wss://aquago.batipy.dev/"
private const val RESUME_DEBOUNCE_MS = 20_000L

/**
 * One persistent WS connection per logged-in session to `ws/orders/feed/`
 * (see OrderFeedConsumer on the backend) — pings whenever anything about the
 * current user's orders changes: a client placing/getting a new order, a
 * status change, a courier getting one assigned. Every screen that shows
 * order data (home banner, orders list, order detail, courier home) collects
 * [updates] and re-runs its own existing REST refresh, instead of each
 * screen opening its own socket.
 */
class OrderFeedRepository(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
    private val applicationScope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updates: SharedFlow<Unit> = _updates.asSharedFlow()

    private var socket: WebSocket? = null
    private var wantsConnection = false

    // Tracked so onAppResumed() can tell a genuinely-dropped connection from
    // a socket that was open the whole time — ON_RESUME fires far more often
    // than "the app was actually backgrounded" (a heads-up notification
    // banner, a permission dialog, anything that briefly steals focus all
    // trigger it too), and forcing a reconnect + full-app refresh on every
    // one of those was flooding the API and tripping its rate limit.
    @Volatile private var isConnected = false
    private var lastResumeHandledAt = 0L

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (webSocket !== socket) return
            isConnected = true
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== socket) return
            _updates.tryEmit(Unit)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket !== socket) return
            isConnected = false
            if (code == 4401) refreshAndReconnect() else scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== socket) return
            isConnected = false
            if (response?.code == 4401) refreshAndReconnect() else scheduleReconnect()
        }
    }

    /** Idempotent — safe to call every time the user is confirmed logged in. */
    fun connect() {
        if (wantsConnection) return
        wantsConnection = true
        openSocket()
    }

    /** Call on logout so a later connect() re-authenticates as the new user. */
    fun disconnect() {
        wantsConnection = false
        socket?.close(1000, null)
        socket = null
    }

    /**
     * Call whenever the app returns to the foreground. Android kills
     * background sockets fairly aggressively (Doze/App Standby), so a status
     * change that happens while the app was genuinely backgrounded for a
     * while can silently miss both the ping (no live connection to receive
     * it on) and the connection itself (needs re-establishing).
     *
     * Two guards keep this from firing on every trivial ON_RESUME (a
     * notification banner, a permission dialog, anything that briefly steals
     * focus also triggers it): skip entirely if the socket is still actually
     * connected (nothing could have been missed), and debounce so rapid
     * repeat resumes only trigger one reconnect + refresh, not one each.
     */
    fun onAppResumed() {
        if (!wantsConnection || isConnected) return
        val now = System.currentTimeMillis()
        if (now - lastResumeHandledAt < RESUME_DEBOUNCE_MS) return
        lastResumeHandledAt = now
        _updates.tryEmit(Unit)
        openSocket()
    }

    private fun openSocket() {
        val requestBuilder = Request.Builder().url("${WS_BASE_URL}ws/orders/feed/")
        tokenStore.currentTokens?.access?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        socket = client.newWebSocket(requestBuilder.build(), listener)
    }

    private fun scheduleReconnect() {
        applicationScope.launch {
            delay(5000)
            if (wantsConnection) openSocket()
        }
    }

    /**
     * A 4401 close here doesn't necessarily mean "logged out" — it's the
     * same code the server sends for a merely-expired access token, and this
     * socket never goes through TokenAuthenticator's normal refresh-on-401
     * flow. Refresh first and only give up if the refresh token itself is
     * no longer valid (genuinely logged out).
     */
    private fun refreshAndReconnect() {
        applicationScope.launch {
            if (wantsConnection && authRepository.refreshAccessToken()) {
                openSocket()
            }
        }
    }
}
