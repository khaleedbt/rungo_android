package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val kind: String,
    @SerialName("base_fare_usd") val baseFareUsd: String,
    val image: String? = null
)

@Serializable
data class PaginatedServicesDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<ServiceDto>
)

@Serializable
data class MerchantDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val logo: String? = null,
    @SerialName("city_name") val cityName: String = "",
    @SerialName("delivery_fee_usd") val deliveryFeeUsd: String
)

@Serializable
data class PaginatedMerchantsDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<MerchantDto>
)

@Serializable
data class CityDto(
    val id: Int,
    val name: String
)

@Serializable
data class PaginatedCitiesDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<CityDto>
)
