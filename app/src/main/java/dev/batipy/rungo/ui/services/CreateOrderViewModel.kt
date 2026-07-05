package dev.batipy.rungo.ui.services

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
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
        val exchangeRates: Map<String, Double> = emptyMap(),
        val selectedCityId: Int? = null,
        // Only used for "delivery" (A→B) services — the pickup side of the trip.
        val selectedPickupLocationId: Int? = null,
        val manualPickupAddress: String = "",
        // For "visit" services this is the client's own address; for "delivery"
        // services it's the drop-off address.
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
    private val ordersRepository: OrdersRepository,
    private val context: Context
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
            // Best-effort: exchange rates aren't critical, so a failed fetch
            // just leaves non-USD prices unconverted rather than blocking the form.
            val exchangeRates = catalogRepository.getExchangeRates().getOrDefault(emptyMap())

            if (cities == null || locations == null) {
                _uiState.value = CreateOrderUiState.LoadError(context.getString(R.string.order_form_load_error))
                return@launch
            }

            val defaultCityId = user?.cityId ?: cities.firstOrNull()?.id
            _uiState.value = CreateOrderUiState.Ready(
                cities = cities,
                locations = locations,
                exchangeRates = exchangeRates,
                selectedCityId = defaultCityId
            )
        }
    }

    fun selectCity(cityId: Int) = updateReady { it.copy(selectedCityId = cityId) }

    fun selectPickupLocation(locationId: Int) = updateReady {
        it.copy(selectedPickupLocationId = locationId, manualPickupAddress = "")
    }

    fun selectPickupManualEntry() = updateReady {
        it.copy(selectedPickupLocationId = null)
    }

    fun updatePickupManualAddress(text: String) = updateReady { it.copy(manualPickupAddress = text) }

    fun selectLocation(locationId: Int) = updateReady {
        it.copy(selectedLocationId = locationId, manualAddress = "")
    }

    fun selectManualEntry() = updateReady {
        it.copy(selectedLocationId = null)
    }

    fun updateManualAddress(text: String) = updateReady { it.copy(manualAddress = text) }

    fun updateComment(text: String) = updateReady { it.copy(comment = text) }

    fun selectCurrency(currency: String) = updateReady { it.copy(currency = currency) }

    fun submit(serviceId: Int, serviceKind: String) {
        val state = _uiState.value as? CreateOrderUiState.Ready ?: return
        val cityId = state.selectedCityId

        val dropoffLocation = state.locations.find { it.id == state.selectedLocationId }
        val dropoffAddress = dropoffLocation?.label?.ifBlank { "${dropoffLocation.latitude}, ${dropoffLocation.longitude}" }
            ?: state.manualAddress

        val isDelivery = serviceKind == "delivery"
        val pickupLocation = state.locations.find { it.id == state.selectedPickupLocationId }
        val pickupAddress = if (isDelivery) {
            pickupLocation?.label?.ifBlank { "${pickupLocation.latitude}, ${pickupLocation.longitude}" }
                ?: state.manualPickupAddress
        } else {
            ""
        }

        if (cityId == null || dropoffAddress.isBlank() || (isDelivery && pickupAddress.isBlank())) {
            _uiState.value = state.copy(error = context.getString(R.string.order_form_address_required))
            return
        }

        _uiState.value = state.copy(submitting = true, error = null)
        viewModelScope.launch {
            val request = OrderCreateRequest(
                service = serviceId,
                city = cityId,
                pickupAddress = pickupAddress,
                pickupLatitude = if (isDelivery) pickupLocation?.latitude else null,
                pickupLongitude = if (isDelivery) pickupLocation?.longitude else null,
                deliveryAddress = dropoffAddress,
                deliveryLatitude = dropoffLocation?.latitude,
                deliveryLongitude = dropoffLocation?.longitude,
                comment = state.comment,
                currency = state.currency
            )
            ordersRepository.createOrder(request)
                .onSuccess { _orderCreated.value = true }
                .onFailure {
                    _uiState.value = state.copy(submitting = false, error = context.getString(R.string.order_form_create_error))
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
        private val ordersRepository: OrdersRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateOrderViewModel(catalogRepository, profileRepository, ordersRepository, context) as T
        }
    }
}
