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
    @SerialName("delivery_address") val deliveryAddress: String,
    @SerialName("delivery_latitude") val deliveryLatitude: String? = null,
    @SerialName("delivery_longitude") val deliveryLongitude: String? = null,
    val comment: String = "",
    val currency: String = "usd"
)

@Serializable
data class OrderCreateResponseDto(val id: Int = 0)
