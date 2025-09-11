package com.nikdi.authms.classes

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class AuthUser(@Contextual val userId: UUID)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AccessToken(
    @SerialName("access_token") val accessToken: String
)

@Serializable
data class RefreshToken(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class TokenIntrospectResponse(
    val active: Boolean,
    val payload: TokenPayload? = null
)

@Serializable
data class TokenPayload(
    val sub: String,
    val userId: String,
    val exp: Long
)

@Serializable
data class Error(
    @SerialName("error") val message: String
)

@Serializable
data class Success(
    val message: String
)

@Serializable
data class UserCredentials(
    val username: String,
    val password: String
)