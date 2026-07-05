package dev.batipy.rungo.data.profile

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.EmptyRequestBody
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.SupportRequest
import dev.batipy.rungo.data.network.dto.UpdateLangRequest
import dev.batipy.rungo.data.network.dto.UserDto
import retrofit2.HttpException
import retrofit2.Response

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

    suspend fun requestLocationViaBot(): Result<Unit> =
        runCatching { api.requestLocation(EmptyRequestBody()).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "requestLocation failed", it) }

    suspend fun updateLanguage(lang: String): Result<UserDto> =
        runCatching { api.updateMe(UpdateLangRequest(lang)) }
            .onFailure { Log.e(TAG, "updateLanguage failed", it) }

    suspend fun sendSupportMessage(message: String): Result<Unit> =
        runCatching { api.sendSupport(SupportRequest(message)).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "sendSupportMessage failed", it) }
}
