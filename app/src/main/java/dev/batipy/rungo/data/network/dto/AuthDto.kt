package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val password2: String,
    @SerialName("full_name") val fullName: String? = null,
    val phone: String? = null
)

@Serializable
data class RegisterTokensDto(
    val access: String,
    val refresh: String
)

@Serializable
data class RegisterResponseDto(
    val id: Int = 0,
    val username: String = "",
    val tokens: RegisterTokensDto
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
    val access: String,
    // Present because the backend has ROTATE_REFRESH_TOKENS=True (see
    // SIMPLE_JWT in core/settings/base.py) — every refresh call issues a new
    // refresh token too, not just a new access token. Nullable defensively
    // in case rotation is ever turned off server-side, in which case the
    // caller should keep reusing the existing refresh token.
    val refresh: String? = null
)
