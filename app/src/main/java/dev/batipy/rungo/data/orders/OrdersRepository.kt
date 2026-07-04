package dev.batipy.rungo.data.orders

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderDto

private const val TAG = "OrdersRepository"

class OrdersRepository(private val api: RunGoApi) {
    suspend fun getRecentOrders(limit: Int = 20): Result<List<OrderDto>> =
        runCatching { api.getOrders().results.take(limit) }
            .onFailure { Log.e(TAG, "getRecentOrders failed", it) }

    suspend fun createOrder(request: OrderCreateRequest): Result<Int> =
        runCatching { api.createOrder(request).id }
            .onFailure { Log.e(TAG, "createOrder failed", it) }
}
