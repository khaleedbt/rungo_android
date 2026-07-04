package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class TokenResponse(
    val access: String,
    val refresh: String
)

@Serializable
data class TokenRefreshRequest(
    val refresh: String
)

@Serializable
data class TokenRefreshResponse(
    val access: String
)
