package dev.batipy.rungo.ui.services

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.ACTIVE_ORDER_STATUSES
import dev.batipy.rungo.data.network.dto.MerchantDto
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.network.dto.ServiceDto
import dev.batipy.rungo.data.orders.OrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ServicesUiState {
    data object Loading : ServicesUiState
    data class Success(
        val services: List<ServiceDto>,
        val merchants: List<MerchantDto>,
        val activeOrder: OrderDto? = null
    ) : ServicesUiState
    data class Error(val message: String) : ServicesUiState
}

class ServicesViewModel(
    private val catalogRepository: CatalogRepository,
    private val ordersRepository: OrdersRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServicesUiState>(ServicesUiState.Loading)
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ServicesUiState.Loading
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

    private suspend fun fetch(): ServicesUiState {
        val services = catalogRepository.getServices().getOrNull()
        val merchants = catalogRepository.getMerchants().getOrNull()
        val activeOrder = ordersRepository.getRecentOrders(limit = 5).getOrNull()
            ?.firstOrNull { it.status in ACTIVE_ORDER_STATUSES }

        return if (services != null && merchants != null) {
            ServicesUiState.Success(services, merchants, activeOrder)
        } else {
            ServicesUiState.Error(context.getString(R.string.services_load_error))
        }
    }

    class Factory(
        private val catalogRepository: CatalogRepository,
        private val ordersRepository: OrdersRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServicesViewModel(catalogRepository, ordersRepository, context) as T
        }
    }
}
