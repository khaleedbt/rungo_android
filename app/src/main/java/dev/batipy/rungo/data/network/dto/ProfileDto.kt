package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int = 0,
    val username: String,
    @SerialName("full_name") val fullName: String = "",
    val phone: String? = null,
    val email: String? = null,
    @SerialName("city") val cityId: Int? = null,
    @SerialName("city_name") val cityName: String = "",
    val role: String,
    val balance: String,
    val lang: String = "ru",
    @SerialName("is_available") val isAvailable: Boolean = false,
    @SerialName("merchant_name") val merchantName: String? = null
)

@Serializable
data class CourierAvailabilityRequest(@SerialName("is_available") val isAvailable: Boolean)

@Serializable
data class CourierAvailabilityResponse(@SerialName("is_available") val isAvailable: Boolean)

@Serializable
data class UpdateProfileRequest(
    val lang: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val city: Int? = null
)

@Serializable
data class LocationDto(
    val id: Int,
    val label: String = "",
    val latitude: String,
    val longitude: String
)

@Serializable
data class LocationCreateRequest(
    val label: String = "",
    val latitude: String,
    val longitude: String
)

@Serializable
data class PaginatedLocationsDto(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<LocationDto>
)

@Serializable
data class SupportRequest(val message: String)
