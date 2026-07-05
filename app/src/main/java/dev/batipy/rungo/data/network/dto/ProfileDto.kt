package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val username: String,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("city") val cityId: Int? = null,
    @SerialName("city_name") val cityName: String = "",
    val role: String,
    val balance: String,
    val lang: String = "ru"
)

@Serializable
data class UpdateLangRequest(val lang: String)

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
