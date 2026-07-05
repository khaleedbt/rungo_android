package dev.batipy.rungo.data.network

import dev.batipy.rungo.data.network.dto.ConfirmDeliveryRequest
import dev.batipy.rungo.data.network.dto.EmptyRequestBody
import dev.batipy.rungo.data.network.dto.LocationCreateRequest
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.LoginRequest
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderCreateResponseDto
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.network.dto.PaginatedCitiesDto
import dev.batipy.rungo.data.network.dto.ReviewCreateRequest
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
import kotlinx.serialization.json.JsonObject
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

    @GET("api/v1/orders/{id}/")
    suspend fun getOrderDetail(@Path("id") id: Int): OrderDetailDto

    @POST("api/v1/orders/{id}/cancel/")
    suspend fun cancelOrder(@Path("id") id: Int, @Body request: EmptyRequestBody): Response<Void>

    @POST("api/v1/orders/{id}/confirm-delivery/")
    suspend fun confirmDelivery(@Path("id") id: Int, @Body request: ConfirmDeliveryRequest): Response<Void>

    @POST("api/v1/orders/{id}/review/")
    suspend fun submitReview(@Path("id") id: Int, @Body request: ReviewCreateRequest): Response<Void>

    @GET("api/v1/cities/")
    suspend fun getCities(): PaginatedCitiesDto

    // Response shape isn't documented in the OpenAPI schema (no serializer),
    // so we decode it as a raw JSON object and probe for known keys.
    @GET("api/v1/exchange-rate/")
    suspend fun getExchangeRate(): JsonObject

    @GET("api/v1/auth/me/")
    suspend fun getMe(): UserDto

    @PATCH("api/v1/auth/me/")
    suspend fun updateMe(@Body request: UpdateLangRequest): UserDto

    @GET("api/v1/locations/")
    suspend fun getLocations(): PaginatedLocationsDto

    @DELETE("api/v1/locations/{id}/")
    suspend fun deleteLocation(@Path("id") id: Int): Response<Void>

    @POST("api/v1/locations/")
    suspend fun createLocation(@Body request: LocationCreateRequest): LocationDto

    @POST("api/v1/support/")
    suspend fun sendSupport(@Body request: SupportRequest): Response<Void>
}
