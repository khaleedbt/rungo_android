package dev.batipy.rungo.data.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.DeviceTokenRequest
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import retrofit2.Response

private const val TAG = "NotificationRepository"

private fun Response<*>.throwIfUnsuccessful() {
    if (!isSuccessful) throw HttpException(this)
}

class NotificationRepository(private val api: RunGoApi) {

    /** Best-effort: called on login and app startup, so a failure here shouldn't block the user. */
    suspend fun registerCurrentDeviceToken(): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        registerDeviceToken(token).getOrThrow()
    }.onFailure { Log.e(TAG, "registerCurrentDeviceToken failed", it) }

    suspend fun registerDeviceToken(token: String): Result<Unit> =
        runCatching { api.registerDeviceToken(DeviceTokenRequest(token)).throwIfUnsuccessful() }
            .onFailure { Log.e(TAG, "registerDeviceToken failed", it) }

    /** Best-effort: called on logout, must not prevent the user from logging out. */
    suspend fun deleteCurrentDeviceToken(): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        api.deleteDeviceToken(DeviceTokenRequest(token)).throwIfUnsuccessful()
    }.onFailure { Log.e(TAG, "deleteCurrentDeviceToken failed", it) }
}
