package dev.batipy.rungo.ui.services

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.location.LocationProvider
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
        val locations: List<LocationDto>,
        val exchangeRates: Map<String, Double> = emptyMap(),
        val selectedCityId: Int? = null,
        // Only used for "delivery" (A→B) services — the pickup side of the
        // trip. Always free text, never picked from the client's own saved
        // locations: point A is typically someone else's address (a shop, a
        // friend's place), not the client's home/work — offering the same
        // "my saved addresses" chips here as for drop-off just invited
        // picking the same address for both ends by habit.
        val manualPickupAddress: String = "",
        // For "visit" services this is the client's own address; for "delivery"
        // services it's the drop-off address.
        val selectedLocationId: Int? = null,
        val manualAddress: String = "",
        val comment: String = "",
        val currency: String = "usd",
        val submitting: Boolean = false,
        val error: String? = null,
        // Transient, snackbar-style — distinct from `error` so it doesn't get
        // rendered with the persistent validation-error styling.
        val message: String? = null
    ) : CreateOrderUiState
}

class CreateOrderViewModel(
    private val catalogRepository: CatalogRepository,
    private val profileRepository: ProfileRepository,
    private val ordersRepository: OrdersRepository,
    private val locationProvider: LocationProvider,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateOrderUiState>(CreateOrderUiState.Loading)
    val uiState: StateFlow<CreateOrderUiState> = _uiState.asStateFlow()

    private val _orderCreated = MutableStateFlow(false)
    val orderCreated: StateFlow<Boolean> = _orderCreated.asStateFlow()

    private val _addingCurrentLocation = MutableStateFlow(false)
    val addingCurrentLocation: StateFlow<Boolean> = _addingCurrentLocation.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = CreateOrderUiState.Loading
            val locations = profileRepository.getLocations().getOrNull()
            val user = profileRepository.getMe().getOrNull()
            // Best-effort: exchange rates aren't critical, so a failed fetch
            // just leaves non-USD prices unconverted rather than blocking the form.
            val exchangeRates = catalogRepository.getExchangeRates().getOrDefault(emptyMap())

            if (locations == null) {
                _uiState.value = CreateOrderUiState.LoadError(context.getString(R.string.order_form_load_error))
                return@launch
            }

            // The order's city always follows the user's own profile city — no
            // in-form picker, since services are already filtered to that city.
            _uiState.value = CreateOrderUiState.Ready(
                locations = locations,
                exchangeRates = exchangeRates,
                selectedCityId = user?.cityId
            )
        }
    }

    fun updatePickupManualAddress(text: String) = updateReady { it.copy(manualPickupAddress = text) }

    fun selectLocation(locationId: Int) = updateReady {
        it.copy(selectedLocationId = locationId, manualAddress = "")
    }

    fun selectManualEntry() = updateReady {
        it.copy(selectedLocationId = null)
    }

    fun updateManualAddress(text: String) = updateReady { it.copy(manualAddress = text) }

    // Captures the device's current GPS position, saves it as a new
    // ClientLocation on the client's profile (same as the "Добавить текущую
    // геолокацию" button on the Profile screen — ProfileViewModel.addCurrentLocation),
    // and immediately selects it — as the drop-off address for "delivery"
    // services, or as the client's own address for "visit" services (both
    // just write into the same selectedLocationId/manualAddress pair).
    fun addCurrentLocation() {
        val current = _uiState.value as? CreateOrderUiState.Ready ?: return
        _addingCurrentLocation.value = true
        viewModelScope.launch {
            val coords = locationProvider.getCurrentLocation()
            if (coords == null) {
                _addingCurrentLocation.value = false
                updateReady { it.copy(error = context.getString(R.string.profile_location_request_error)) }
                return@launch
            }
            val (latitude, longitude) = coords
            profileRepository.createLocation(latitude, longitude)
                .onSuccess { newLocation ->
                    updateReady {
                        it.copy(
                            locations = it.locations + newLocation,
                            selectedLocationId = newLocation.id,
                            manualAddress = "",
                            message = context.getString(R.string.current_location_saved_hint)
                        )
                    }
                }
                .onFailure {
                    updateReady { state -> state.copy(error = context.getString(R.string.profile_location_request_error)) }
                }
            _addingCurrentLocation.value = false
        }
    }

    fun locationPermissionDenied() = updateReady {
        it.copy(error = context.getString(R.string.profile_location_permission_denied))
    }

    fun consumeMessage() = updateReady { it.copy(message = null) }

    fun updateComment(text: String) = updateReady { it.copy(comment = text) }

    fun selectCurrency(currency: String) = updateReady { it.copy(currency = currency) }

    fun submit(serviceId: Int, serviceKind: String) {
        val state = _uiState.value as? CreateOrderUiState.Ready ?: return
        val cityId = state.selectedCityId

        val dropoffLocation = state.locations.find { it.id == state.selectedLocationId }
        val dropoffAddress = dropoffLocation?.label?.ifBlank { "${dropoffLocation.latitude}, ${dropoffLocation.longitude}" }
            ?: state.manualAddress

        val isDelivery = serviceKind == "delivery"
        val pickupAddress = if (isDelivery) state.manualPickupAddress else ""

        if (cityId == null) {
            _uiState.value = state.copy(error = context.getString(R.string.order_form_city_required))
            return
        }
        if (dropoffAddress.isBlank() || (isDelivery && pickupAddress.isBlank())) {
            _uiState.value = state.copy(error = context.getString(R.string.order_form_address_required))
            return
        }

        _uiState.value = state.copy(submitting = true, error = null)
        viewModelScope.launch {
            val request = OrderCreateRequest(
                service = serviceId,
                city = cityId,
                pickupAddress = pickupAddress,
                pickupLatitude = null,
                pickupLongitude = null,
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
        private val locationProvider: LocationProvider,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CreateOrderViewModel(catalogRepository, profileRepository, ordersRepository, locationProvider, context) as T
        }
    }
}
