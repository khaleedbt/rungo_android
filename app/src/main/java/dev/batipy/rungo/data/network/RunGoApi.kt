package dev.batipy.rungo.data.network

import dev.batipy.rungo.data.network.dto.LoginRequest
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderCreateResponseDto
import dev.batipy.rungo.data.network.dto.PaginatedCitiesDto
import dev.batipy.rungo.data.network.dto.PaginatedLocationsDto
import dev.batipy.rungo.data.network.dto.PaginatedMerchantsDto
import dev.batipy.rungo.data.network.dto.PaginatedOrdersDto
import dev.batipy.rungo.data.network.dto.PaginatedServicesDto
import dev.batipy.rungo.data.network.dto.SupportRequest
import dev.batipy.rungo.data.network.dto.TokenRefreshRequest
import dev.batipy.rungo.data.network.dto.TokenRefreshResponse
import dev.batipy.rungo.data.network.dto.TokenResponse
import dev.batipy.rungo.data.network.dto.UpdateLangRequest
import dev.batipy.rungo.data.network.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface RunGoApi {
    @POST("api/v1/auth/login/")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("api/v1/auth/refresh/")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): TokenRefreshResponse

    @GET("api/v1/services/")
    suspend fun getServices(): PaginatedServicesDto

    @GET("api/v1/merchants/")
    suspend fun getMerchants(): PaginatedMerchantsDto

    @GET("api/v1/orders/")
    suspend fun getOrders(): PaginatedOrdersDto

    @POST("api/v1/orders/")
    suspend fun createOrder(@Body request: OrderCreateRequest): OrderCreateResponseDto

    @GET("api/v1/cities/")
    suspend fun getCities(): PaginatedCitiesDto

    @GET("api/v1/auth/me/")
    suspend fun getMe(): UserDto

    @PATCH("api/v1/auth/me/")
    suspend fun updateMe(@Body request: UpdateLangRequest): UserDto

    @GET("api/v1/locations/")
    suspend fun getLocations(): PaginatedLocationsDto

    @DELETE("api/v1/locations/{id}/")
    suspend fun deleteLocation(@Path("id") id: Int): Response<Void>

    @POST("api/v1/auth/request-location/")
    suspend fun requestLocation(): Response<Void>

    @POST("api/v1/support/")
    suspend fun sendSupport(@Body request: SupportRequest): Response<Void>
}
