package dev.batipy.rungo.ui.courier

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.location.CourierLocationService
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.orders.OrderFeedRepository
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    orderFeedRepository: OrderFeedRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourierOrdersUiState>(CourierOrdersUiState.Loading)
    val uiState: StateFlow<CourierOrdersUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
        // Live "an order changed" feed (see OrderFeedRepository/OrderFeedConsumer)
        // — pings, no payload, just re-triggers the same refresh() a manual
        // pull-to-refresh would, so the list updates without the courier having
        // to pull down themselves.
        viewModelScope.launch {
            orderFeedRepository.updates.collect { refresh() }
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
                .onSuccess {
                    // Taking an order moves it straight to "in_progress"
                    // server-side (CourierOrderTakeView), which means it now
                    // needs live GPS — but this courier may never open that
                    // order's own detail screen (CourierOrderDetailViewModel
                    // is what normally starts the tracking service), so start
                    // it right here instead of leaving tracking to chance.
                    CourierLocationService.start(context, orderId)
                    load()
                }
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

    class Factory(
        private val ordersRepository: OrdersRepository,
        private val profileRepository: ProfileRepository,
        private val orderFeedRepository: OrderFeedRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourierOrdersViewModel(ordersRepository, profileRepository, orderFeedRepository, context) as T
        }
    }
}
