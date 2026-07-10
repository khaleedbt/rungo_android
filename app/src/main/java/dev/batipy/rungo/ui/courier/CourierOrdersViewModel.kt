package dev.batipy.rungo.ui.courier

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.chat.ChatRepository
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

// The courier/orders/ endpoint already scopes results to exactly these two
// buckets server-side (confirmed+unassigned, or assigned to this courier), so
// bucketing on the client is a plain status split — no need for a `courier`
// field to disambiguate "available" from "mine".
private val ACTIVE_COURIER_STATUSES = setOf("in_progress", "in_delivery")
private val HISTORY_COURIER_STATUSES = setOf("delivered", "cancelled")

sealed interface CourierOrdersUiState {
    data object Loading : CourierOrdersUiState
    data class Error(val message: String) : CourierOrdersUiState
    data class Success(
        val availableOrders: List<OrderDto>,
        val activeOrders: List<OrderDto>,
        val historyOrders: List<OrderDto>,
        val isAvailable: Boolean,
        val updatingAvailability: Boolean = false,
        // A set (not a single nullable id) so taking two different orders in
        // quick succession tracks each in-flight request independently —
        // otherwise the second tap would clear the first order's loading
        // state while its own take() call is still racing.
        val takingOrderIds: Set<Int> = emptySet()
    ) : CourierOrdersUiState
}

class CourierOrdersViewModel(
    private val ordersRepository: OrdersRepository,
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourierOrdersUiState>(CourierOrdersUiState.Loading)
    val uiState: StateFlow<CourierOrdersUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Live "an assigned order changed" feed (see CourierFeedConsumer on the
    // backend) — carries no payload, just triggers the same refresh() a
    // manual pull-to-refresh would, so the list updates without the courier
    // having to pull down themselves. Best-effort: if it drops, the courier
    // still has pull-to-refresh as a fallback, so reconnects are silent and
    // don't surface an error state.
    private var feedSocket: WebSocket? = null

    private val feedListener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== feedSocket) return
            refresh()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (webSocket !== feedSocket) return
            if (code != 4401 && code != 4403) scheduleFeedReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== feedSocket) return
            val code = response?.code
            if (code != 4401 && code != 4403) scheduleFeedReconnect()
        }
    }

    init {
        load()
        feedSocket = chatRepository.connectCourierFeed(feedListener)
    }

    private fun scheduleFeedReconnect() {
        viewModelScope.launch {
            delay(5000)
            feedSocket = chatRepository.connectCourierFeed(feedListener)
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CourierOrdersUiState.Loading
            _uiState.value = fetch()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = fetch()
            _isRefreshing.value = false
        }
    }

    private suspend fun fetch(): CourierOrdersUiState {
        val orders = ordersRepository.getCourierOrders().getOrNull()
        val user = profileRepository.getMe().getOrNull()
        return if (orders != null && user != null) {
            CourierOrdersUiState.Success(
                availableOrders = orders.filter { it.status == "confirmed" },
                activeOrders = orders.filter { it.status in ACTIVE_COURIER_STATUSES },
                historyOrders = orders.filter { it.status in HISTORY_COURIER_STATUSES },
                isAvailable = user.isAvailable
            )
        } else {
            CourierOrdersUiState.Error(context.getString(R.string.courier_orders_load_error))
        }
    }

    fun toggleAvailability() {
        val current = _uiState.value as? CourierOrdersUiState.Success ?: return
        _uiState.value = current.copy(updatingAvailability = true)
        viewModelScope.launch {
            profileRepository.setCourierAvailability(!current.isAvailable)
                .onSuccess { isAvailable ->
                    _uiState.value = (_uiState.value as? CourierOrdersUiState.Success)
                        ?.copy(isAvailable = isAvailable, updatingAvailability = false)
                        ?: _uiState.value
                }
                .onFailure {
                    _uiState.value = (_uiState.value as? CourierOrdersUiState.Success)
                        ?.copy(updatingAvailability = false)
                        ?: _uiState.value
                    _message.value = context.getString(R.string.courier_availability_error)
                }
        }
    }

    fun takeOrder(orderId: Int) {
        val current = _uiState.value as? CourierOrdersUiState.Success ?: return
        if (orderId in current.takingOrderIds) return
        _uiState.value = current.copy(takingOrderIds = current.takingOrderIds + orderId)
        viewModelScope.launch {
            ordersRepository.takeCourierOrder(orderId)
                .onSuccess { load() }
                .onFailure {
                    val latest = _uiState.value as? CourierOrdersUiState.Success
                    _uiState.value = latest?.copy(takingOrderIds = latest.takingOrderIds - orderId) ?: _uiState.value
                    _message.value = context.getString(R.string.courier_take_order_error)
                }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    override fun onCleared() {
        super.onCleared()
        feedSocket?.close(1000, null)
    }

    class Factory(
        private val ordersRepository: OrdersRepository,
        private val profileRepository: ProfileRepository,
        private val chatRepository: ChatRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourierOrdersViewModel(ordersRepository, profileRepository, chatRepository, context) as T
        }
    }
}
