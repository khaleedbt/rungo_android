package dev.batipy.rungo.data.network

import dev.batipy.rungo.data.network.dto.ConfirmDeliveryRequest
import dev.batipy.rungo.data.network.dto.CourierAvailabilityRequest
import dev.batipy.rungo.data.network.dto.CourierAvailabilityResponse
import dev.batipy.rungo.data.network.dto.DeviceTokenRequest
import dev.batipy.rungo.data.network.dto.EmptyRequestBody
import dev.batipy.rungo.data.network.dto.LocationCreateRequest
import dev.batipy.rungo.data.network.dto.LocationDto
import dev.batipy.rungo.data.network.dto.LoginRequest
import dev.batipy.rungo.data.network.dto.MerchantDetailDto
import dev.batipy.rungo.data.network.dto.OrderCreateRequest
import dev.batipy.rungo.data.network.dto.OrderCreateResponseDto
import dev.batipy.rungo.data.network.dto.OrderDetailDto
import dev.batipy.rungo.data.network.dto.OrderLocationRequest
import dev.batipy.rungo.data.network.dto.OrderStatusRequest
import dev.batipy.rungo.data.network.dto.PaginatedCitiesDto
import dev.batipy.rungo.data.network.dto.ReviewCreateRequest
import dev.batipy.rungo.data.network.dto.PaginatedLocationsDto
import dev.batipy.rungo.data.network.dto.PaginatedMerchantsDto
import dev.batipy.rungo.data.network.dto.PaginatedOrdersDto
import dev.batipy.rungo.data.network.dto.PaginatedServicesDto
import dev.batipy.rungo.data.network.dto.RegisterRequest
import dev.batipy.rungo.data.network.dto.RegisterResponseDto
import dev.batipy.rungo.data.network.dto.SupportRequest
import dev.batipy.rungo.data.network.dto.TokenRefreshRequest
import dev.batipy.rungo.data.network.dto.TokenRefreshResponse
import dev.batipy.rungo.data.network.dto.TokenResponse
import dev.batipy.rungo.data.network.dto.UpdateProfileRequest
import dev.batipy.rungo.data.network.dto.UserDto
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface RunGoApi {
    @POST("api/v1/auth/login/")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("api/v1/auth/refresh/")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): TokenRefreshResponse

    @POST("api/v1/auth/register/")
    suspend fun register(@Body request: RegisterRequest): RegisterResponseDto

    @GET("api/v1/services/")
    suspend fun getServices(@Query("city") cityId: Int? = null): PaginatedServicesDto

    @GET("api/v1/merchants/")
    suspend fun getMerchants(@Query("city") cityId: Int? = null): PaginatedMerchantsDto

    @GET("api/v1/merchants/{id}/")
    suspend fun getMerchant(@Path("id") id: Int): MerchantDetailDto

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

    @GET("api/v1/courier/orders/")
    suspend fun getCourierOrders(): PaginatedOrdersDto

    @GET("api/v1/partner/orders/")
    suspend fun getPartnerOrders(): PaginatedOrdersDto

    @POST("api/v1/courier/orders/{id}/take/")
    suspend fun takeCourierOrder(@Path("id") id: Int): OrderDetailDto

    @POST("api/v1/courier/orders/{id}/release/")
    suspend fun releaseCourierOrder(@Path("id") id: Int): OrderDetailDto

    @PATCH("api/v1/courier/orders/{id}/status/")
    suspend fun updateCourierOrderStatus(@Path("id") id: Int, @Body request: OrderStatusRequest): OrderDetailDto

    @POST("api/v1/courier/orders/{id}/collect-payment/")
    suspend fun collectPayment(@Path("id") id: Int): OrderDetailDto

    @POST("api/v1/courier/orders/{id}/location/")
    suspend fun postCourierLocation(@Path("id") id: Int, @Body request: OrderLocationRequest): Response<Void>

    @PATCH("api/v1/courier/availability/")
    suspend fun setCourierAvailability(@Body request: CourierAvailabilityRequest): CourierAvailabilityResponse

    @GET("api/v1/cities/")
    suspend fun getCities(): PaginatedCitiesDto

    // Response shape isn't documented in the OpenAPI schema (no serializer),
    // so we decode it as a raw JSON object and probe for known keys.
    @GET("api/v1/exchange-rate/")
    suspend fun getExchangeRate(): JsonObject

    @GET("api/v1/auth/me/")
    suspend fun getMe(): UserDto

    @PATCH("api/v1/auth/me/")
    suspend fun updateMe(@Body request: UpdateProfileRequest): UserDto

    @GET("api/v1/locations/")
    suspend fun getLocations(): PaginatedLocationsDto

    @DELETE("api/v1/locations/{id}/")
    suspend fun deleteLocation(@Path("id") id: Int): Response<Void>

    @POST("api/v1/locations/")
    suspend fun createLocation(@Body request: LocationCreateRequest): LocationDto

    @POST("api/v1/support/")
    suspend fun sendSupport(@Body request: SupportRequest): Response<Void>

    @POST("api/v1/device-token/")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): Response<Void>

    @HTTP(method = "DELETE", path = "api/v1/device-token/", hasBody = true)
    suspend fun deleteDeviceToken(@Body request: DeviceTokenRequest): Response<Void>
}
