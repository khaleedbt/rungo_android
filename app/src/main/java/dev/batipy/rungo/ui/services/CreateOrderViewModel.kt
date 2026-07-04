package dev.batipy.rungo.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.CityDto
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateOrderUiState {
    data object Loading : CreateOrderUiState
    data class LoadError(val message: String) : CreateOrderUiState
    data class Ready(
        val cities: List<CityDto>,
        val locations: List<LocationDto>,
        val selectedCityId: Int? = null,
        val selectedLocationId: Int? = null,
        val manualAddress: String = "",
        val comment: String = "",
        val currency: String = "usd",
        val submitting: Boolean = false,
        val error: String? = null
    ) : CreateOrderUiState
}

class CreateOrderViewModel(
    private val catalogRepository: CatalogRepository,
    private val profileRepository: ProfileRepository,
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateOrderUiState>(CreateOrderUiState.Loading)
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private val _orderCreated = MutableStateFlow(false)
    val orderCreated: StateFlow<Boolean> = _orderCreated.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = CreateOrderUiState.Loading
            val cities = catalogRepository.getCities().getOrNull()
            val locations = profileRepository.getLocations().getOrNull()
            val user = profileRepository.getMe().getOrNull()

            if (cities == null || locations == null) {
                _uiState.value = CreateOrderUiState.LoadError("Не удалось загрузить данные для заказа")
                return@launch
            }

            val defaultCityId = user?.cityId ?: cities.firstOrNull()?.id
            _uiState.value = CreateOrderUiState.Ready(
                cities = cities,
                locations = locations,
                selectedCityId = defaultCityId
            )
        }
    }

    fun selectCity(cityId: Int) = updateReady { it.copy(selectedCityId = cityId) }

    fun selectLocation(locationId: Int) = updateReady {
        it.copy(selectedLocationId = locationId, manualAddress = "")
    }

    fun selectManualEntry() = updateReady {
        it.copy(selectedLocationId = null)
    }

    fun updateManualAddress(text: String) = updateReady { it.copy(manualAddress = text) }

    fun updateComment(text: String) = updateReady { it.copy(comment = text) }

    fun selectCurrency(currency: String) = updateReady { it.copy(currency = currency) }

    fun submit(serviceId: Int) {
        val state = _uiState.value as? CreateOrderUiState.Ready ?: return
        val cityId = state.selectedCityId
        val selectedLocation = state.locations.find { it.id == state.selectedLocationId }
        val address = selectedLocation?.label?.ifBlank { "${selectedLocation.latitude}, ${selectedLocation.longitude}" }
            ?: state.manualAddress

        if (cityId == null || address.isBlank()) {
            _uiState.value = state.copy(error = "Укажите город и адрес")
            return
        }

        _uiState.value = state.copy(submitting = true, error = null)
        viewModelScope.launch {
            val request = OrderCreateRequest(
                service = serviceId,
                city = cityId,
                deliveryAddress = address,
                deliveryLatitude = selectedLocation?.latitude,
                deliveryLongitude = selectedLocation?.longitude,
                comment = state.comment,
                currency = state.currency
            )
            ordersRepository.createOrder(request)
                .onSuccess { _orderCreated.value = true }
                .onFailure {
                    _uiState.value = state.copy(submitting = false, error = "Не удалось создать заказ")
                }
        }
    }

    private inline fun updateReady(block: (CreateOrderUiState.Ready) -> CreateOrderUiState.Ready) {
        val current = _uiState.value as? CreateOrderUiState.Ready ?: return
        _uiState.value = block(current)
    }

    class Factory(
        private val catalogRepository: CatalogRepository,
        private val profileRepository: ProfileRepository,
        private val ordersRepository: OrdersRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateOrderViewModel(catalogRepository, profileRepository, ordersRepository) as T
        }
    }
}
