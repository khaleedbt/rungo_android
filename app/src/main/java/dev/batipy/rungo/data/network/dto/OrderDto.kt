package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDto(
    val id: Int,
    @SerialName("city_name") val cityName: String = "",
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("cod_total") val codTotal: String,
    val currency: String = "usd",
    @SerialName("service_name") val serviceName: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String? = null
)

val ACTIVE_ORDER_STATUSES = setOf("new", "confirmed", "in_progress", "in_delivery")

@Serializable
data class PaginatedOrdersDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<OrderDto>
)

@Serializable
data class OrderCreateRequest(
    val service: Int? = null,
    val city: Int,
    @SerialName("pickup_address") val pickupAddress: String = "",
    @SerialName("pickup_latitude") val pickupLatitude: String? = null,
    @SerialName("pickup_longitude") val pickupLongitude: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String,
    @SerialName("delivery_latitude") val deliveryLatitude: String? = null,
    @SerialName("delivery_longitude") val deliveryLongitude: String? = null,
    val comment: String = "",
    val currency: String = "usd"
)

@Serializable
data class OrderCreateResponseDto(val id: Int = 0)

@Serializable
data class OrderDetailDto(
    val id: Int,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("service_name") val serviceName: String? = null,
    @SerialName("city_name") val cityName: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("service_fee") val serviceFee: String? = null,
    @SerialName("cod_total") val codTotal: String? = null,
    val currency: String = "usd",
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("courier_full_name") val courierFullName: String? = null,
    @SerialName("courier_username") val courierUsername: String? = null,
    val review: ReviewDto? = null
) {
    val courierDisplayName: String?
        get() = courierFullName?.ifBlank { null } ?: courierUsername?.ifBlank { null }
}

@Serializable
data class ReviewDto(
    val rating: Int = 0,
    val text: String = ""
)

@Serializable
data class ReviewCreateRequest(
    val rating: Int,
    val text: String = ""
)

@Serializable
data class ConfirmDeliveryRequest(
    @SerialName("payment_method") val paymentMethod: String
)
