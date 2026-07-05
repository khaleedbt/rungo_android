package dev.batipy.rungo.ui.shop

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.cart.CartItem
import dev.batipy.rungo.data.cart.CartRepository
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.MerchantDetailDto
import dev.batipy.rungo.data.network.dto.ProductDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MerchantDetailUiState {
    data object Loading : MerchantDetailUiState
    data class Success(val merchant: MerchantDetailDto) : MerchantDetailUiState
    data class Error(val message: String) : MerchantDetailUiState
}

class MerchantDetailViewModel(
    private val merchantId: Int,
    private val catalogRepository: CatalogRepository,
    private val cartRepository: CartRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MerchantDetailUiState>(MerchantDetailUiState.Loading)
    val uiState: StateFlow<MerchantDetailUiState> = _uiState.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = cartRepository.items

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = MerchantDetailUiState.Loading
            _uiState.value = catalogRepository.getMerchant(merchantId).fold(
                onSuccess = { MerchantDetailUiState.Success(it) },
                onFailure = { MerchantDetailUiState.Error(context.getString(R.string.merchant_load_error)) }
            )
        }
    }

    fun addToCart(product: ProductDto) = cartRepository.addItem(product)

    fun updateQuantity(productId: Int, quantity: Int) = cartRepository.updateQuantity(productId, quantity)

    class Factory(
        private val merchantId: Int,
        private val catalogRepository: CatalogRepository,
        private val cartRepository: CartRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MerchantDetailViewModel(merchantId, catalogRepository, cartRepository, context) as T
        }
    }
}
