package dev.batipy.rungo.ui.cart

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.cart.CartItem
import dev.batipy.rungo.data.cart.CartRepository
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.location.LocationProvider
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderItemRequest
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CartUiState {
    data object Loading : CartUiState
    data class Error(val message: String) : CartUiState
    data class Ready(
        val locations: List<LocationDto>,
        val exchangeRates: Map<String, Double> = emptyMap(),
        val selectedCityId: Int? = null,
        val selectedLocationId: Int? = null,
        val manualAddress: String = "",
        val comment: String = "",
        val currency: String = "usd",
        val submitting: Boolean = false,
        val error: String? = null,
        // Transient, snackbar-style — distinct from `error` so it doesn't get
        // rendered with the persistent validation-error styling.
        val message: String? = null
    ) : CartUiState
}

class CartViewModel(
    private val cartRepository: CartRepository,
    private val catalogRepository: CatalogRepository,
    private val ordersRepository: OrdersRepository,
    private val profileRepository: ProfileRepository,
    private val locationProvider: LocationProvider,
    private val context: Context
) : ViewModel() {

    val cartItems: StateFlow<List<CartItem>> = cartRepository.items

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _orderCreated = MutableStateFlow<Int?>(null)
    val orderCreated: StateFlow<Int?> = _orderCreated.asStateFlow()

    private val _addingCurrentLocation = MutableStateFlow(false)
    val addingCurrentLocation: StateFlow<Boolean> = _addingCurrentLocation.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading
            val locations = profileRepository.getLocations().getOrNull()
            val user = profileRepository.getMe().getOrNull()
            // Best-effort: a failed fetch just leaves non-USD prices unconverted.
            val exchangeRates = catalogRepository.getExchangeRates().getOrDefault(emptyMap())

            if (locations == null) {
                _uiState.value = CartUiState.Error(context.getString(R.string.order_form_load_error))
                return@launch
            }

            // The order's city always follows the user's own profile city — no
            // in-form picker, since shops are already filtered to that city.
            _uiState.value = CartUiState.Ready(
                locations = locations,
                exchangeRates = exchangeRates,
                selectedCityId = user?.cityId
            )
        }
    }

    // Locations are fetched once at init, but the user can add/delete them
    // from the Profile tab while a cart draft is already open — re-sync the
    // list (and drop a selection that was deleted) each time Cart is shown.
    fun refreshLocations() {
        viewModelScope.launch {
            val locations = profileRepository.getLocations().getOrNull() ?: return@launch
            updateReady { state ->
                val selectionStillValid = state.selectedLocationId?.let { id -> locations.any { it.id == id } } ?: false
                state.copy(
                    locations = locations,
                    selectedLocationId = if (selectionStillValid) state.selectedLocationId else null
                )
            }
        }
    }

    fun selectLocation(locationId: Int) = updateReady {
        it.copy(selectedLocationId = locationId, manualAddress = "")
    }

    fun selectManualEntry() = updateReady { it.copy(selectedLocationId = null) }

    fun updateManualAddress(text: String) = updateReady { it.copy(manualAddress = text) }

    // Same pattern as CreateOrderViewModel.addCurrentLocation — captures the
    // device's current GPS position, saves it as a new ClientLocation on the
    // client's profile, and immediately selects it as the delivery address.
    fun addCurrentLocation() {
        val current = _uiState.value as? CartUiState.Ready ?: return
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

    fun updateItemQuantity(productId: Int, quantity: Int) = cartRepository.updateQuantity(productId, quantity)

    fun removeItem(productId: Int) = cartRepository.removeItem(productId)

    fun clearCart() = cartRepository.clear()

    fun submit() {
        val state = _uiState.value as? CartUiState.Ready ?: return
        val items = cartItems.value
        if (items.isEmpty()) return

        val cityId = state.selectedCityId
        val location = state.locations.find { it.id == state.selectedLocationId }
        val address = location?.label?.ifBlank { "${location.latitude}, ${location.longitude}" }
            ?: state.manualAddress

        if (cityId == null) {
            _uiState.value = state.copy(error = context.getString(R.string.order_form_city_required))
            return
        }
        if (address.isBlank()) {
            _uiState.value = state.copy(error = context.getString(R.string.order_form_address_required))
            return
        }

        _uiState.value = state.copy(submitting = true, error = null)
        viewModelScope.launch {
            val request = OrderCreateRequest(
                city = cityId,
                deliveryAddress = address,
                deliveryLatitude = location?.latitude,
                deliveryLongitude = location?.longitude,
                comment = state.comment,
                currency = state.currency,
                items = items.map { OrderItemRequest(it.product.id, it.quantity) }
            )
            ordersRepository.createOrder(request)
                .onSuccess { orderId ->
                    cartRepository.clear()
                    _orderCreated.value = orderId
                    // Reset the form (address/comment/submitting) for the next
                    // order — otherwise this stale Ready state (submitting =
                    // true forever) resurfaces the moment the cart is non-empty
                    // again, showing the previous order's leftover input with
                    // a permanently spinning submit button.
                    load()
                }
                .onFailure {
                    _uiState.value = state.copy(submitting = false, error = context.getString(R.string.order_form_create_error))
                }
        }
    }

    fun consumeOrderCreated() {
        _orderCreated.value = null
    }

    private inline fun updateReady(block: (CartUiState.Ready) -> CartUiState.Ready) {
        val current = _uiState.value as? CartUiState.Ready ?: return
        _uiState.value = block(current)
    }

    class Factory(
        private val cartRepository: CartRepository,
        private val catalogRepository: CatalogRepository,
        private val ordersRepository: OrdersRepository,
        private val profileRepository: ProfileRepository,
        private val locationProvider: LocationProvider,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CartViewModel(cartRepository, catalogRepository, ordersRepository, profileRepository, locationProvider, context) as T
        }
    }
}
