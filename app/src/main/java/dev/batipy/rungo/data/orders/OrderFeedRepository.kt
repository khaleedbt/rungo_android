package dev.batipy.rungo.data.orders

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
    private val applicationScope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val updates: SharedFlow<Unit> = _updates.asSharedFlow()

    private var socket: WebSocket? = null
    private var wantsConnection = false

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== socket) return
            _updates.tryEmit(Unit)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket !== socket) return
            if (code != 4401) scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== socket) return
            if (response?.code != 4401) scheduleReconnect()
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
     * change that happens while the app is backgrounded can silently miss
     * both the ping (no live connection to receive it on) and the connection
     * itself (needs re-establishing). Forces a fresh reconnect and an
     * immediate "assume something changed" ping so every screen catches up
     * right away instead of waiting for the next real event.
     */
    fun onAppResumed() {
        if (!wantsConnection) return
        _updates.tryEmit(Unit)
        socket?.close(1000, null)
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
}
