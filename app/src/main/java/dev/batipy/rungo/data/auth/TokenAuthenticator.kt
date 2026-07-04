package dev.batipy.rungo.data.auth

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.TokenRefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

private const val TAG = "TokenAuthenticator"
private const val MAX_RETRIES = 2

/**
 * Refreshes the access token on a 401 using a plain [refreshApi] client that
 * bypasses [AuthInterceptor]/this authenticator, to avoid a recursive refresh loop.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val refreshApi: RunGoApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRIES) return null
        val refreshToken = tokenStore.currentTokens?.refresh ?: return null

        return runBlocking {
            try {
                val newAccess = refreshApi.refreshToken(TokenRefreshRequest(refreshToken)).access
                tokenStore.updateAccessToken(newAccess)
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            } catch (e: Exception) {
                Log.e(TAG, "Token refresh failed, logging out", e)
                tokenStore.clearTokens()
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
