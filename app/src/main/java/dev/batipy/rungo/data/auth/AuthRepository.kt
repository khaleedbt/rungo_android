package dev.batipy.rungo.data.auth

import android.content.Context
import dev.batipy.rungo.R
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.LoginRequest
import dev.batipy.rungo.data.network.dto.RegisterRequest
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
}
