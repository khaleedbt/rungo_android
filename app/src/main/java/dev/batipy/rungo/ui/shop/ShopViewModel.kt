package dev.batipy.rungo.ui.shop

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.catalog.CatalogRepository
import dev.batipy.rungo.data.network.dto.MerchantDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ShopUiState {
    data object Loading : ShopUiState
    data class Success(val merchants: List<MerchantDto>) : ShopUiState
    data class Error(val message: String) : ShopUiState
}

class ShopViewModel(
    private val catalogRepository: CatalogRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShopUiState>(ShopUiState.Loading)
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ShopUiState.Loading
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

    private suspend fun fetch(): ShopUiState = catalogRepository.getMerchants().fold(
        onSuccess = { ShopUiState.Success(it) },
        onFailure = { ShopUiState.Error(context.getString(R.string.shop_load_error)) }
    )

    class Factory(
        private val catalogRepository: CatalogRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ShopViewModel(catalogRepository, context) as T
        }
    }
}
