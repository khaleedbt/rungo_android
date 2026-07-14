package dev.batipy.rungo.ui.partner

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.dto.ACTIVE_ORDER_STATUSES
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.orders.OrderFeedRepository
import dev.batipy.rungo.data.orders.OrdersRepository
import dev.batipy.rungo.data.profile.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

data class PartnerStats(val deliveredCount: Int, val revenue: Double)

sealed interface PartnerOrdersUiState {
    data object Loading : PartnerOrdersUiState
    data class Error(val message: String) : PartnerOrdersUiState
    data class Success(
        val merchantName: String?,
        val activeOrders: List<OrderDto>,
        val historyOrders: List<OrderDto>,
        val statsToday: PartnerStats,
        val statsMonth: PartnerStats,
        val statsAllTime: PartnerStats
    ) : PartnerOrdersUiState
}

class PartnerOrdersViewModel(
    private val ordersRepository: OrdersRepository,
    private val profileRepository: ProfileRepository,
    orderFeedRepository: OrderFeedRepository,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PartnerOrdersUiState>(PartnerOrdersUiState.Loading)
    val uiState: StateFlow<PartnerOrdersUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        load()
        // Live "an order changed" ping (see OrderFeedRepository) — the
        // miniapp polls every 15s for this same screen, but the app already
        // has a push-based feed for every other order screen, so reuse it
        // here too instead of adding a second, redundant polling mechanism.
        viewModelScope.launch {
            orderFeedRepository.updates.collect { refresh() }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = PartnerOrdersUiState.Loading
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

    private suspend fun fetch(): PartnerOrdersUiState {
        val orders = ordersRepository.getPartnerOrders().getOrNull()
        val user = profileRepository.getMe().getOrNull()
        return if (orders != null && user != null) {
            val delivered = orders.filter { it.status == "delivered" }
            val today = LocalDate.now()
            PartnerOrdersUiState.Success(
                merchantName = user.merchantName,
                activeOrders = orders.filter { it.status in ACTIVE_ORDER_STATUSES },
                historyOrders = orders.filter { it.status !in ACTIVE_ORDER_STATUSES },
                statsToday = statsSince(delivered) { it == today },
                statsMonth = statsSince(delivered) { it.month == today.month && it.year == today.year },
                statsAllTime = statsSince(delivered) { true }
            )
        } else {
            PartnerOrdersUiState.Error(context.getString(R.string.partner_orders_load_error))
        }
    }

    private fun statsSince(delivered: List<OrderDto>, inWindow: (LocalDate) -> Boolean): PartnerStats {
        val matching = delivered.filter { order -> orderDate(order.createdAt)?.let(inWindow) == true }
        val revenue = matching.sumOf { it.goodsAmount?.toDoubleOrNull() ?: 0.0 }
        return PartnerStats(deliveredCount = matching.size, revenue = revenue)
    }

    private fun orderDate(iso: String): LocalDate? = try {
        OffsetDateTime.parse(iso).toLocalDate()
    } catch (e: DateTimeParseException) {
        null
    }

    class Factory(
        private val ordersRepository: OrdersRepository,
        private val profileRepository: ProfileRepository,
        private val orderFeedRepository: OrderFeedRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PartnerOrdersViewModel(ordersRepository, profileRepository, orderFeedRepository, context) as T
        }
    }
}
