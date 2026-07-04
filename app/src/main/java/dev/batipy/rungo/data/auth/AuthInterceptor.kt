package dev.batipy.rungo.data.auth

import okhttp3.Interceptor
import okhttp3.Response

private val NO_AUTH_PATHS = listOf(
    "api/v1/auth/login/",
    "api/v1/auth/register/",
    "api/v1/auth/refresh/"
)

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath.removePrefix("/")
        val accessToken = tokenStore.currentTokens?.access

        if (path in NO_AUTH_PATHS || accessToken == null) {
            return chain.proceed(original)
        }

        val authorized = original.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        return chain.proceed(authorized)
    }
}
