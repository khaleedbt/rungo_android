package dev.batipy.rungo.data.profile

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.LocationCreateRequest
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.SupportRequest
import dev.batipy.rungo.data.network.dto.UpdateProfileRequest
import dev.batipy.rungo.data.network.dto.UserDto
import retrofit2.HttpException
import retrofit2.Response
import java.util.Locale

private const val TAG = "ProfileRepository"

// See OrdersRepository.throwIfUnsuccessful — Response<T> return types don't
// auto-throw on non-2xx, so this must be checked explicitly or failures are
// silently swallowed as "success".
private fun Response<*>.throwIfUnsuccessful() {
    if (!isSuccessful) throw HttpException(this)
}

class ProfileRepository(private val api: RunGoApi) {

    suspend fun getMe(): Result<UserDto> =
        runCatching { api.getMe() }
            .onFailure { Log.e(TAG, "getMe failed", it) }

    suspend fun getLocations(): Result<List<LocationDto>> =
        runCatching { api.getLocations().results }
            .onFailure { Log.e(TAG, "getLocations failed", it) }

    suspend fun deleteLocation(id: Int): Result<Unit> =
        runCatching { api.deleteLocation(id).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "deleteLocation failed", it) }

    suspend fun createLocation(latitude: Double, longitude: Double): Result<LocationDto> =
        runCatching {
            api.createLocation(
                LocationCreateRequest(
                    latitude = String.format(Locale.US, "%.6f", latitude),
                    longitude = String.format(Locale.US, "%.6f", longitude)
                )
            )
        }.onFailure { Log.e(TAG, "createLocation failed", it) }

    suspend fun updateLanguage(lang: String): Result<UserDto> =
        runCatching { api.updateMe(UpdateProfileRequest(lang = lang)) }
            .onFailure { Log.e(TAG, "updateLanguage failed", it) }

    suspend fun updateProfile(fullName: String, phone: String, email: String, cityId: Int?): Result<UserDto> =
        runCatching {
            api.updateMe(
                UpdateProfileRequest(
                    fullName = fullName,
                    phone = phone.ifBlank { null },
                    email = email.ifBlank { null },
                    city = cityId
                )
            )
        }.onFailure { Log.e(TAG, "updateProfile failed", it) }

    suspend fun updateCity(cityId: Int): Result<UserDto> =
        runCatching { api.updateMe(UpdateProfileRequest(city = cityId)) }
            .onFailure { Log.e(TAG, "updateCity failed", it) }

    suspend fun sendSupportMessage(message: String): Result<Unit> =
        runCatching { api.sendSupport(SupportRequest(message)).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "sendSupportMessage failed", it) }
}
