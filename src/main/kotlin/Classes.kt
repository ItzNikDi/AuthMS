package com.nikdi

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val username: String,
    val password: String
)

@Serializable
data class AuthUser(val userId: Int)