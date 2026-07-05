package dev.batipy.rungo.data.cart

import dev.batipy.rungo.data.network.dto.ProductDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CartItem(val product: ProductDto, val quantity: Int)

/**
 * Holds the shop cart in memory for the lifetime of the process — scoped at
 * the Application level (see RunGoApplication) rather than to any single
 * ViewModel, so it survives switching between the Shop and Cart tabs.
 */
class CartRepository {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun quantityOf(productId: Int): Int =
        _items.value.find { it.product.id == productId }?.quantity ?: 0

    fun addItem(product: ProductDto) {
        _items.update { current ->
            val existing = current.find { it.product.id == product.id }
            if (existing != null) {
                current.map { if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                current + CartItem(product, 1)
            }
        }
    }

    fun updateQuantity(productId: Int, quantity: Int) {
        if (quantity <= 0) {
            removeItem(productId)
            return
        }
        _items.update { current ->
            current.map { if (it.product.id == productId) it.copy(quantity = quantity) else it }
        }
    }

    fun removeItem(productId: Int) {
        _items.update { current -> current.filterNot { it.product.id == productId } }
    }

    fun clear() {
        _items.value = emptyList()
    }
}
