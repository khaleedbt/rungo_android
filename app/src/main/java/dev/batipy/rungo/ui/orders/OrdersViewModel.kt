package dev.batipy.rungo.ui.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.orders.OrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrdersUiState {
    data object Loading : OrdersUiState
    data class Success(val orders: List<OrderDto>) : OrdersUiState
    data class Error(val message: String) : OrdersUiState
}

class OrdersViewModel(
    private val ordersRepository: OrdersRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = OrdersUiState.Loading
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

    private suspend fun fetch(): OrdersUiState = ordersRepository.getRecentOrders(limit = 20).fold(
        onSuccess = { OrdersUiState.Success(it) },
        onFailure = { OrdersUiState.Error(context.getString(R.string.orders_load_error)) }
    )

    class Factory(
        private val ordersRepository: OrdersRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrdersViewModel(ordersRepository, context) as T
        }
    }
}
