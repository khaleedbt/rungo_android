package dev.batipy.rungo.data.orders

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.ConfirmDeliveryRequest
import dev.batipy.rungo.data.network.dto.EmptyRequestBody
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.network.dto.OrderDto
import dev.batipy.rungo.data.network.dto.OrderLocationRequest
import dev.batipy.rungo.data.network.dto.OrderStatusRequest
import dev.batipy.rungo.data.network.dto.ReviewCreateRequest
import retrofit2.HttpException
import retrofit2.Response
import java.util.Locale

private const val TAG = "OrdersRepository"

/**
 * Retrofit only auto-throws on a non-2xx response when the return type is the
 * body itself — for `Response<T>` return types (used here to dodge empty-body
 * parsing issues) a failed HTTP call completes normally with isSuccessful =
 * false. Without this check, cancel/confirm/review calls would silently
 * report success even when the server rejected them.
 */
private fun Response<*>.throwIfUnsuccessful() {
    if (!isSuccessful) throw HttpException(this)
}

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
        runCatching { api.cancelOrder(id, EmptyRequestBody()).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "cancelOrder failed", it) }

    suspend fun confirmDelivery(id: Int, paymentMethod: String): Result<Unit> =
        runCatching { api.confirmDelivery(id, ConfirmDeliveryRequest(paymentMethod)).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "confirmDelivery failed", it) }

    suspend fun submitReview(id: Int, rating: Int, text: String): Result<Unit> =
        runCatching { api.submitReview(id, ReviewCreateRequest(rating, text)).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "submitReview failed", it) }

    suspend fun getCourierOrders(): Result<List<OrderDto>> =
        runCatching { api.getCourierOrders().results }
            .onFailure { Log.e(TAG, "getCourierOrders failed", it) }

    suspend fun getPartnerOrders(): Result<List<OrderDto>> =
        runCatching { api.getPartnerOrders().results }
            .onFailure { Log.e(TAG, "getPartnerOrders failed", it) }

    suspend fun takeCourierOrder(id: Int): Result<OrderDetailDto> =
        runCatching { api.takeCourierOrder(id) }
            .onFailure { Log.e(TAG, "takeCourierOrder failed", it) }

    suspend fun releaseCourierOrder(id: Int): Result<OrderDetailDto> =
        runCatching { api.releaseCourierOrder(id) }
            .onFailure { Log.e(TAG, "releaseCourierOrder failed", it) }

    suspend fun updateCourierOrderStatus(id: Int, status: String): Result<OrderDetailDto> =
        runCatching { api.updateCourierOrderStatus(id, OrderStatusRequest(status)) }
            .onFailure { Log.e(TAG, "updateCourierOrderStatus failed", it) }

    suspend fun collectPayment(id: Int): Result<OrderDetailDto> =
        runCatching { api.collectPayment(id) }
            .onFailure { Log.e(TAG, "collectPayment failed", it) }

    // No onFailure logging here — this fires every ~12s while delivering, a
    // transient failure is normal (brief signal loss) and not worth spamming
    // logcat over; the next tick just tries again.
    suspend fun postCourierLocation(id: Int, latitude: Double, longitude: Double, accuracyM: Float?): Result<Unit> =
        runCatching {
            // Double.toString() keeps full precision (often 7+ decimal
            // digits for a GPS fix), but the backend's DecimalField only
            // allows 6 — sending more than that fails validation with a 400
            // on every single ping. Round explicitly instead of relying on
            // however many digits the platform happens to print.
            val lat = String.format(Locale.US, "%.6f", latitude)
            val lng = String.format(Locale.US, "%.6f", longitude)
            api.postCourierLocation(id, OrderLocationRequest(lat, lng, accuracyM)).throwIfUnsuccessful()
        }
}
