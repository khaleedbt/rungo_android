package dev.batipy.rungo.data.chat

import dev.batipy.rungo.data.auth.TokenStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

private const val WS_BASE_URL = "wss://aquago.batipy.dev/"

class ChatRepository(private val tokenStore: TokenStore) {

    // A dedicated client (not the Retrofit one) — WS connections are
    // long-lived and don't go through the REST interceptor/authenticator
    // pipeline, so the auth header is attached directly per-connection below.
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect(orderId: Int, listener: WebSocketListener): WebSocket {
        val requestBuilder = Request.Builder().url("${WS_BASE_URL}ws/orders/$orderId/chat/")
        tokenStore.currentTokens?.access?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        return client.newWebSocket(requestBuilder.build(), listener)
    }
}
