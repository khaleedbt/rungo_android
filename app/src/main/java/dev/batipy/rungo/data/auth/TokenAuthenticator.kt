package dev.batipy.rungo.data.auth

import android.util.Log
import dev.batipy.rungo.data.network.RunGoApi
import dev.batipy.rungo.data.network.dto.TokenRefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException

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
                val refreshResponse = refreshApi.refreshToken(TokenRefreshRequest(refreshToken))
                val newAccess = refreshResponse.access
                tokenStore.updateAccessToken(newAccess, refreshResponse.refresh)
                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            } catch (e: HttpException) {
                // The backend itself rejected the refresh token (expired,
                // revoked) — this session really is over.
                Log.e(TAG, "Refresh token rejected (${e.code()}), logging out", e)
                tokenStore.clearTokens()
                null
            } catch (e: IOException) {
                // A network blip during the refresh call (timeout, dropped
                // connection, no signal) — not a real session failure. Logging
                // the user out over this hits couriers with weak signal
                // hardest; just fail this one request and let the next
                // network call retry the refresh normally.
                Log.w(TAG, "Token refresh failed due to a network error, not logging out", e)
                null
            } catch (e: Exception) {
                // Anything else unexpected — same conservative treatment,
                // since we can't confirm the refresh token was actually
                // rejected rather than something transient going wrong.
                Log.e(TAG, "Token refresh failed unexpectedly, not logging out", e)
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
