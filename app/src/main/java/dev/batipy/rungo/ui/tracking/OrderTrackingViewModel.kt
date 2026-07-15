package dev.batipy.rungo.ui.tracking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.auth.AuthRepository
import dev.batipy.rungo.data.location.CourierLocationFrame
import dev.batipy.rungo.data.location.OrderLocationRepository
import dev.batipy.rungo.data.orders.OrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

sealed interface OrderTrackingUiState {
    data object Loading : OrderTrackingUiState
    data class Error(val message: String) : OrderTrackingUiState
    data class Success(
        val destinationLatitude: Double?,
        val destinationLongitude: Double?,
        val pickupLatitude: Double?,
        val pickupLongitude: Double?,
        val courierName: String?,
        val orderCreatedAt: String,
        val courierLatitude: Double? = null,
        val courierLongitude: Double? = null
    ) : OrderTrackingUiState
}

class OrderTrackingViewModel(
    private val orderId: Int,
    private val ordersRepository: OrdersRepository,
    private val orderLocationRepository: OrderLocationRepository,
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private var socket: WebSocket? = null

    private val _uiState = MutableStateFlow<OrderTrackingUiState>(OrderTrackingUiState.Loading)
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== socket) return
            val frame = try {
                json.decodeFromString(CourierLocationFrame.serializer(), text)
            } catch (e: Exception) {
                return
            }
            if (frame.type != "location") return
            val lat = frame.latitude?.toDoubleOrNull() ?: return
            val lng = frame.longitude?.toDoubleOrNull() ?: return
            updateSuccess { it.copy(courierLatitude = lat, courierLongitude = lng) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket !== socket) return
            handleDisconnect(code)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== socket) return
            handleDisconnect(response?.code)
        }
    }

    // A tracking session can easily outlast the 15-minute access token, and
    // this raw WebSocket never goes through TokenAuthenticator's normal
    // refresh-on-401 flow — without this, live tracking would silently stop
    // updating partway through a delivery. 4403 (not allowed to see this
    // order at all) is never fixable by refreshing a token, so it's terminal.
    private fun handleDisconnect(code: Int?) {
        when (code) {
            4401 -> viewModelScope.launch {
                if (authRepository.refreshAccessToken()) connect()
            }
            4403 -> Unit
            else -> reconnect()
        }
    }

    init {
        load()
        connect()
    }

    fun load() {
        viewModelScope.launch {
            val order = ordersRepository.getOrderDetail(orderId).getOrNull()
            _uiState.value = if (order != null) {
                OrderTrackingUiState.Success(
                    destinationLatitude = order.deliveryLatitude?.toDoubleOrNull(),
                    destinationLongitude = order.deliveryLongitude?.toDoubleOrNull(),
                    pickupLatitude = order.pickupLatitude?.toDoubleOrNull(),
                    pickupLongitude = order.pickupLongitude?.toDoubleOrNull(),
                    courierName = order.courierDisplayName,
                    orderCreatedAt = order.createdAt
                )
            } else {
                OrderTrackingUiState.Error(context.getString(R.string.order_load_error))
            }
        }
    }

    private fun connect() {
        socket?.close(1000, null)
        socket = orderLocationRepository.connect(orderId, listener)
    }

    private fun reconnect() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            connect()
        }
    }

    private inline fun updateSuccess(block: (OrderTrackingUiState.Success) -> OrderTrackingUiState.Success) {
        val current = _uiState.value as? OrderTrackingUiState.Success ?: return
        _uiState.value = block(current)
    }

    override fun onCleared() {
        super.onCleared()
        socket?.close(1000, null)
    }

    class Factory(
        private val orderId: Int,
        private val ordersRepository: OrdersRepository,
        private val orderLocationRepository: OrderLocationRepository,
        private val authRepository: AuthRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderTrackingViewModel(orderId, ordersRepository, orderLocationRepository, authRepository, context) as T
        }
    }
}
