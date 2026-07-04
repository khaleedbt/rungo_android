package dev.batipy.rungo.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dev.batipy.rungo.data.auth.AuthInterceptor
import dev.batipy.rungo.data.auth.TokenAuthenticator
import dev.batipy.rungo.data.auth.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

private const val BASE_URL = "https://aquago.batipy.dev/"

object NetworkModule {

    fun createApi(tokenStore: TokenStore): RunGoApi {
        val json = Json { ignoreUnknownKeys = true }
        val converterFactory = json.asConverterFactory("application/json".toMediaType())

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Bare client/instance used only for token refresh — must not carry
        // AuthInterceptor or the Authenticator, or a failed refresh would recurse.
        val refreshApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(logging).build())
            .addConverterFactory(converterFactory)
            .build()
            .create(RunGoApi::class.java)

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(tokenStore, refreshApi))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(converterFactory)
            .build()

        return retrofit.create(RunGoApi::class.java)
    }
}
