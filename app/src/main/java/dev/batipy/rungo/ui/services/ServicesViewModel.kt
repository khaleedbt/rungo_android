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
import dev.batipy.rungo.data.orders.OrderFeedRepository
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ServicesUiState {
    data object Loading : ServicesUiState
    data class Success(
        val services: List<ServiceDto>,
        val merchants: List<MerchantDto>,
        val activeOrders: List<OrderDto> = emptyList()
    ) : ServicesUiState
    data class Error(val message: String) : ServicesUiState
}

class ServicesViewModel(
    private val catalogRepository: CatalogRepository,
    private val ordersRepository: OrdersRepository,
    private val profileRepository: ProfileRepository,
    orderFeedRepository: OrderFeedRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ServicesUiState>(ServicesUiState.Loading)
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
        // Live "an order changed" ping (see OrderFeedRepository) — so the
        // active-order banner appears/updates as soon as a client places an
        // order or its status changes, without pulling the home screen down.
        viewModelScope.launch {
            orderFeedRepository.updates.collect { refresh() }
        }
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
        val cityId = profileRepository.getMe().getOrNull()?.cityId
        val services = catalogRepository.getServices(cityId).getOrNull()
        val merchants = catalogRepository.getMerchants(cityId).getOrNull()
        val activeOrders = ordersRepository.getRecentOrders(limit = 20).getOrNull()
            ?.filter { it.status in ACTIVE_ORDER_STATUSES }
            .orEmpty()

        return if (services != null && merchants != null) {
            ServicesUiState.Success(services, merchants, activeOrders)
        } else {
            ServicesUiState.Error(context.getString(R.string.services_load_error))
        }
    }

    class Factory(
        private val catalogRepository: CatalogRepository,
        private val ordersRepository: OrdersRepository,
        private val profileRepository: ProfileRepository,
        private val orderFeedRepository: OrderFeedRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ServicesViewModel(catalogRepository, ordersRepository, profileRepository, orderFeedRepository, context) as T
        }
    }
}
