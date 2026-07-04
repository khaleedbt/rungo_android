package dev.batipy.rungo.data.auth

import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.LoginRequest
import retrofit2.HttpException
import java.io.IOException

sealed interface LoginResult {
    data object Success : LoginResult
    data class Error(val message: String) : LoginResult
}

class AuthRepository(
    private val api: RunGoApi,
    private val tokenStore: TokenStore
) {
    suspend fun login(username: String, password: String): LoginResult {
        return try {
            val response = api.login(LoginRequest(username, password))
            tokenStore.saveTokens(TokenPair(response.access, response.refresh))
            LoginResult.Success
        } catch (e: HttpException) {
            val message = if (e.code() == 401) {
                "Неверный логин или пароль"
            } else {
                "Ошибка сервера (${e.code()})"
            }
            LoginResult.Error(message)
        } catch (e: IOException) {
            LoginResult.Error("Нет соединения с сервером")
        }
    }

    suspend fun logout() {
        tokenStore.clearTokens()
    }

    suspend fun isLoggedIn(): Boolean = tokenStore.awaitInitialTokens() != null
}
