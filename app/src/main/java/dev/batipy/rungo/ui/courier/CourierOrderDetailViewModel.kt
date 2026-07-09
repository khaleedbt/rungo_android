package dev.batipy.rungo.ui.courier

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.orders.OrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CourierOrderDetailUiState {
    data object Loading : CourierOrderDetailUiState
    data class Error(val message: String) : CourierOrderDetailUiState
    data class Success(
        val order: OrderDetailDto,
        val performingAction: Boolean = false
    ) : CourierOrderDetailUiState
}

class CourierOrderDetailViewModel(
    private val orderId: Int,
    private val ordersRepository: OrdersRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourierOrderDetailUiState>(CourierOrderDetailUiState.Loading)
    val uiState: StateFlow<CourierOrderDetailUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // No init{load()} here — HomeScreen.kt always calls load() via
    // LaunchedEffect(orderId) right after obtaining this ViewModel, both for
    // a freshly created instance and one reused from the ViewModelStore.
    // Loading here too would fire a redundant getOrderDetail() on every open.

    fun load() {
        viewModelScope.launch {
            _uiState.value = CourierOrderDetailUiState.Loading
            _uiState.value = fetch()
        }
    }

    private suspend fun fetch(): CourierOrderDetailUiState {
        val order = ordersRepository.getOrderDetail(orderId).getOrNull()
            ?: return CourierOrderDetailUiState.Error(context.getString(R.string.order_load_error))
        return CourierOrderDetailUiState.Success(order)
    }

    fun takeOrder() = performAction { ordersRepository.takeCourierOrder(orderId) }

    fun markInDelivery() = performAction { ordersRepository.updateCourierOrderStatus(orderId, "in_delivery") }

    fun releaseOrder() = performAction { ordersRepository.releaseCourierOrder(orderId) }

    fun collectPayment() = performAction { ordersRepository.collectPayment(orderId) }

    private fun performAction(action: suspend () -> Result<OrderDetailDto>) {
        val current = _uiState.value as? CourierOrderDetailUiState.Success ?: return
        _uiState.value = current.copy(performingAction = true)
        viewModelScope.launch {
            action()
                .onSuccess { updated -> _uiState.value = CourierOrderDetailUiState.Success(updated) }
                .onFailure {
                    _uiState.value = (_uiState.value as? CourierOrderDetailUiState.Success)
                        ?.copy(performingAction = false)
                        ?: _uiState.value
                    _message.value = context.getString(R.string.courier_action_error)
                }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    class Factory(
        private val orderId: Int,
        private val ordersRepository: OrdersRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourierOrderDetailViewModel(orderId, ordersRepository, context) as T
        }
    }
}
