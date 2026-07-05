package dev.batipy.rungo.ui.orders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrderDetailUiState {
    data object Loading : OrderDetailUiState
    data class Success(
        val order: OrderDetailDto,
        val userBalance: String? = null,
        val exchangeRates: Map<String, Double> = emptyMap(),
        val showingPaymentPicker: Boolean = false,
        val cancelling: Boolean = false,
        val confirming: Boolean = false,
        val reviewRating: Int = 0,
        val reviewText: String = "",
        val submittingReview: Boolean = false
    ) : OrderDetailUiState
    data class Error(val message: String) : OrderDetailUiState
}

class OrderDetailViewModel(
    private val orderId: Int,
    private val ordersRepository: OrdersRepository,
    private val profileRepository: ProfileRepository,
    private val catalogRepository: CatalogRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState.Loading
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

    private suspend fun fetch(): OrderDetailUiState {
        val order = ordersRepository.getOrderDetail(orderId).getOrNull()
            ?: return OrderDetailUiState.Error(context.getString(R.string.order_load_error))
        val balance = profileRepository.getMe().getOrNull()?.balance
        // Best-effort: balance is stored in USD, so non-USD orders need this to
        // check whether the balance covers the total; a failed fetch just means
        // we can't verify it (handled as "insufficient" by the UI to be safe).
        val exchangeRates = catalogRepository.getExchangeRates().getOrDefault(emptyMap())
        return OrderDetailUiState.Success(order = order, userBalance = balance, exchangeRates = exchangeRates)
    }

    fun cancelOrder() {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        _uiState.value = current.copy(cancelling = true)
        viewModelScope.launch {
            ordersRepository.cancelOrder(orderId)
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = current.copy(cancelling = false)
                    _message.value = context.getString(R.string.order_cancel_error)
                }
        }
    }

    fun requestConfirmDelivery() {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        _uiState.value = current.copy(showingPaymentPicker = true)
    }

    fun cancelConfirmDelivery() {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        _uiState.value = current.copy(showingPaymentPicker = false)
    }

    fun confirmDelivery(paymentMethod: String) {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        _uiState.value = current.copy(confirming = true)
        viewModelScope.launch {
            ordersRepository.confirmDelivery(orderId, paymentMethod)
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = current.copy(confirming = false, showingPaymentPicker = false)
                    _message.value = context.getString(R.string.order_confirm_error)
                }
        }
    }

    fun selectReviewRating(rating: Int) {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        _uiState.value = current.copy(reviewRating = rating)
    }

    fun updateReviewText(text: String) {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        _uiState.value = current.copy(reviewText = text)
    }

    fun submitReview() {
        val current = _uiState.value as? OrderDetailUiState.Success ?: return
        if (current.reviewRating <= 0) {
            _message.value = context.getString(R.string.review_rating_required)
            return
        }
        _uiState.value = current.copy(submittingReview = true)
        viewModelScope.launch {
            ordersRepository.submitReview(orderId, current.reviewRating, current.reviewText)
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = current.copy(submittingReview = false)
                    _message.value = context.getString(R.string.review_submit_error)
                }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    class Factory(
        private val orderId: Int,
        private val ordersRepository: OrdersRepository,
        private val profileRepository: ProfileRepository,
        private val catalogRepository: CatalogRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrderDetailViewModel(orderId, ordersRepository, profileRepository, catalogRepository, context) as T
        }
    }
}
