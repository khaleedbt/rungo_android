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

    // OkHttp calls authenticate() once per failed request, on whatever
    // dispatcher thread that request was running on — with several screens
    // (and a WebSocket reconnect) all hitting the API around the same time,
    // an expired access token means N concurrent 401s, and without this lock
    // each one independently POSTed to /auth/refresh/ at once. The backend
    // rate-limits that endpoint, so the 2nd/3rd/4th simultaneous refresh came
    // back 429 — which was then (wrongly) treated as "refresh token
    // rejected" and logged the user out, even though the token itself was
    // perfectly fine and the 1st request's refresh had already succeeded.
    private val refreshLock = Any()

    // Set right after a refresh attempt for a given (now-stale) access token
    // fails for a reason that isn't "the refresh token itself is invalid" —
    // a 429, a 5xx, a network blip. Lets a second thread that was waiting on
    // the lock for that same expired token skip straight to failing this
    // round too, instead of immediately retrying the exact call that just
    // failed and piling more requests onto whatever the server is already
    // struggling with. Paired with a short cooldown (below) rather than
    // blocking that token forever — an earlier version of this cleared only
    // on success, which meant one transient failure permanently wedged every
    // later request carrying that same token (it never changes without a
    // successful refresh) into failing instantly with no further attempts,
    // for the rest of that access token's 15-minute life.
    private var lastFailedAccessToken: String? = null
    private var lastFailureAtMs: Long = 0L
    private val retryCooldownMs = 2000L

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_RETRIES) return null

        synchronized(refreshLock) {
            val failedAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val current = tokenStore.currentTokens ?: return null

            // Another thread already refreshed while this one was waiting
            // for the lock — the store no longer holds the token that just
            // failed, so just retry with what's there now instead of hitting
            // the network a second time for the same expiry.
            if (current.access != failedAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${current.access}")
                    .build()
            }

            if (failedAccessToken != null && failedAccessToken == lastFailedAccessToken &&
                System.currentTimeMillis() - lastFailureAtMs < retryCooldownMs
            ) {
                return null
            }

            return runBlocking {
                try {
                    val refreshResponse = refreshApi.refreshToken(TokenRefreshRequest(current.refresh))
                    val newAccess = refreshResponse.access
                    tokenStore.updateAccessToken(newAccess, refreshResponse.refresh)
                    lastFailedAccessToken = null
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccess")
                        .build()
                } catch (e: HttpException) {
                    if (e.code() == 401) {
                        // The backend itself rejected the refresh token
                        // (expired, revoked) — this session really is over.
                        Log.e(TAG, "Refresh token rejected (401), logging out", e)
                        tokenStore.clearTokens()
                    } else {
                        // Anything else (429 rate-limit, a 5xx hiccup, ...)
                        // is the server having a bad moment, not the refresh
                        // token being invalid — don't log the user out over it.
                        Log.w(TAG, "Token refresh failed with HTTP ${e.code()}, not logging out", e)
                        lastFailedAccessToken = failedAccessToken
                        lastFailureAtMs = System.currentTimeMillis()
                    }
                    null
                } catch (e: IOException) {
                    // A network blip during the refresh call (timeout, dropped
                    // connection, no signal) — not a real session failure. Logging
                    // the user out over this hits couriers with weak signal
                    // hardest; just fail this one request and let the next
                    // network call retry the refresh normally.
                    Log.w(TAG, "Token refresh failed due to a network error, not logging out", e)
                    lastFailedAccessToken = failedAccessToken
                    lastFailureAtMs = System.currentTimeMillis()
                    null
                } catch (e: Exception) {
                    // Anything else unexpected — same conservative treatment,
                    // since we can't confirm the refresh token was actually
                    // rejected rather than something transient going wrong.
                    Log.e(TAG, "Token refresh failed unexpectedly, not logging out", e)
                    lastFailedAccessToken = failedAccessToken
                    lastFailureAtMs = System.currentTimeMillis()
                    null
                }
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
