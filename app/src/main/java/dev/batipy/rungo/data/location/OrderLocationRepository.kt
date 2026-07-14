package dev.batipy.rungo.data.location

import dev.batipy.rungo.data.auth.TokenStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

private const val WS_BASE_URL = "wss://aquago.batipy.dev/"

@Serializable
data class CourierLocationFrame(
    val type: String,
    val latitude: String? = null,
    val longitude: String? = null,
    @SerialName("recorded_at") val recordedAt: String? = null
)

/** Read-only live feed of a courier's GPS pings for one order — see
 * OrderLocationConsumer on the backend. Separate socket from ChatRepository's
 * (different lifecycle: opened only while the tracking screen is visible,
 * not for the whole session). */
class OrderLocationRepository(private val tokenStore: TokenStore) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect(orderId: Int, listener: WebSocketListener): WebSocket {
        val requestBuilder = Request.Builder().url("${WS_BASE_URL}ws/orders/$orderId/location/")
        tokenStore.currentTokens?.access?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        return client.newWebSocket(requestBuilder.build(), listener)
    }
}
