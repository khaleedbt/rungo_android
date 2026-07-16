package dev.batipy.rungo.data.auth

import android.content.Context
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.LoginRequest
import dev.batipy.rungo.data.network.dto.RegisterRequest
import dev.batipy.rungo.data.network.dto.TokenRefreshRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginResult {
    data object Success : LoginResult
    data class Error(val message: String) : LoginResult
}

sealed interface RegisterResult {
    data object Success : RegisterResult
    data class Error(val message: String) : RegisterResult
}

class AuthRepository(
    private val api: RunGoApi,
    private val tokenStore: TokenStore,
    private val context: Context
) {
    suspend fun login(username: String, password: String): LoginResult {
        return try {
            val response = api.login(LoginRequest(username, password))
            tokenStore.saveTokens(TokenPair(response.access, response.refresh))
            LoginResult.Success
        } catch (e: HttpException) {
            val message = if (e.code() == 401) {
                context.getString(R.string.login_error_invalid)
            } else {
                context.getString(R.string.login_error_server, e.code())
            }
            LoginResult.Error(message)
        } catch (e: IOException) {
            LoginResult.Error(context.getString(R.string.login_error_no_connection))
        }
    }

    suspend fun register(
        username: String,
        password: String,
        password2: String,
        fullName: String?,
        phone: String?
    ): RegisterResult {
        return try {
            val response = api.register(
                RegisterRequest(
                    username = username,
                    password = password,
                    password2 = password2,
                    fullName = fullName?.ifBlank { null },
                    phone = phone?.ifBlank { null }
                )
            )
            tokenStore.saveTokens(TokenPair(response.tokens.access, response.tokens.refresh))
            RegisterResult.Success
        } catch (e: HttpException) {
            val message = parseErrorBody(e.response()?.errorBody()?.string())
                ?: context.getString(R.string.register_error_generic)
            RegisterResult.Error(message)
        } catch (e: IOException) {
            RegisterResult.Error(context.getString(R.string.login_error_no_connection))
        }
    }

    /** DRF validation errors look like {"field": ["message"]} or {"detail": "message"}. */
    private fun parseErrorBody(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val obj = Json.parseToJsonElement(body) as? JsonObject ?: return null
            when (val first = obj.values.firstOrNull()) {
                is JsonArray -> (first.firstOrNull() as? JsonPrimitive)?.content
                is JsonPrimitive -> first.content
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logout() {
        tokenStore.clearTokens()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.awaitInitialTokens() != null

    /**
     * Long-lived WebSocket connections (chat, order feed, live tracking)
     * carry a bearer token set once at connect time and never go through
     * TokenAuthenticator's automatic-refresh-on-401 — over a long enough
     * session the 15-minute access token expires under them and every
     * reconnect attempt gets rejected (WSREJECT / close code 4401) until the
     * app is restarted. Call this before retrying such a connection so it
     * reconnects with a fresh token instead of failing forever.
     *
     * Safe to call directly on [api]: api/v1/auth/refresh/ is in
     * AuthInterceptor's NO_AUTH_PATHS, so this never attaches the (possibly
     * already-expired) access token, and never touches TokenAuthenticator.
     */
    suspend fun refreshAccessToken(): Boolean {
        val refresh = tokenStore.currentTokens?.refresh ?: return false
        return try {
            val response = api.refreshToken(TokenRefreshRequest(refresh))
            tokenStore.updateAccessToken(response.access, response.refresh)
            true
        } catch (e: Exception) {
            false
        }
    }
}
