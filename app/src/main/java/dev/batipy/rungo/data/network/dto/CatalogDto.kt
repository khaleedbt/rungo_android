package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceDto(
    val id: Int,
    val name: String,
    @SerialName("name_en") val nameEn: String? = null,
    @SerialName("name_ar") val nameAr: String? = null,
    val description: String = "",
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("description_ar") val descriptionAr: String? = null,
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
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("description_ar") val descriptionAr: String? = null,
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
data class ProductDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val image: String? = null,
    @SerialName("price_usd") val priceUsd: String,
    val merchant: Int = 0,
    @SerialName("merchant_name") val merchantName: String = "",
    @SerialName("merchant_delivery_fee_usd") val merchantDeliveryFeeUsd: String = "0"
)

@Serializable
data class CategoryDto(
    val id: Int? = null,
    val name: String = "",
    @SerialName("name_en") val nameEn: String? = null,
    @SerialName("name_ar") val nameAr: String? = null,
    val products: List<ProductDto> = emptyList()
)

@Serializable
data class MerchantDetailDto(
    val id: Int,
    val name: String,
    val description: String = "",
    @SerialName("description_en") val descriptionEn: String? = null,
    @SerialName("description_ar") val descriptionAr: String? = null,
    val logo: String? = null,
    val categories: List<CategoryDto> = emptyList()
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
