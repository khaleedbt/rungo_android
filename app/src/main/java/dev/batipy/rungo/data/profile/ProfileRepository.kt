package dev.batipy.rungo.data.profile

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.SupportRequest
import dev.batipy.rungo.data.network.dto.UpdateLangRequest
import dev.batipy.rungo.data.network.dto.UserDto

private const val TAG = "ProfileRepository"

class ProfileRepository(private val api: RunGoApi) {

    suspend fun getMe(): Result<UserDto> =
        runCatching { api.getMe() }
            .onFailure { Log.e(TAG, "getMe failed", it) }

    suspend fun getLocations(): Result<List<LocationDto>> =
        runCatching { api.getLocations().results }
            .onFailure { Log.e(TAG, "getLocations failed", it) }

    suspend fun deleteLocation(id: Int): Result<Unit> =
        runCatching { api.deleteLocation(id) }
            .onFailure { Log.e(TAG, "deleteLocation failed", it) }
            .map {}

    suspend fun requestLocationViaBot(): Result<Unit> =
        runCatching { api.requestLocation() }
            .onFailure { Log.e(TAG, "requestLocation failed", it) }
            .map {}

    suspend fun updateLanguage(lang: String): Result<UserDto> =
        runCatching { api.updateMe(UpdateLangRequest(lang)) }
            .onFailure { Log.e(TAG, "updateLanguage failed", it) }

    suspend fun sendSupportMessage(message: String): Result<Unit> =
        runCatching { api.sendSupport(SupportRequest(message)) }
            .onFailure { Log.e(TAG, "sendSupportMessage failed", it) }
            .map {}
}
