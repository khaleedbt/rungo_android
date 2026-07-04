package dev.batipy.rungo.data.orders

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.ConfirmDeliveryRequest
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.network.dto.ReviewCreateRequest

private const val TAG = "OrdersRepository"

class OrdersRepository(private val api: RunGoApi) {
    suspend fun getRecentOrders(limit: Int = 20): Result<List<OrderDto>> =
        runCatching { api.getOrders().results.take(limit) }
            .onFailure { Log.e(TAG, "getRecentOrders failed", it) }

    suspend fun createOrder(request: OrderCreateRequest): Result<Int> =
        runCatching { api.createOrder(request).id }
            .onFailure { Log.e(TAG, "createOrder failed", it) }

    suspend fun getOrderDetail(id: Int): Result<OrderDetailDto> =
        runCatching { api.getOrderDetail(id) }
            .onFailure { Log.e(TAG, "getOrderDetail failed", it) }

    suspend fun cancelOrder(id: Int): Result<Unit> =
        runCatching { api.cancelOrder(id) }
            .onFailure { Log.e(TAG, "cancelOrder failed", it) }
            .map {}

    suspend fun confirmDelivery(id: Int, paymentMethod: String): Result<Unit> =
        runCatching { api.confirmDelivery(id, ConfirmDeliveryRequest(paymentMethod)) }
            .onFailure { Log.e(TAG, "confirmDelivery failed", it) }
            .map {}

    suspend fun submitReview(id: Int, rating: Int, text: String): Result<Unit> =
        runCatching { api.submitReview(id, ReviewCreateRequest(rating, text)) }
            .onFailure { Log.e(TAG, "submitReview failed", it) }
            .map {}
}
