package com.nikdi

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

const val JWT_ENV = "JWT_SECRET"
const val REALM_ENV = "KTOR_REALM"

object JwtConfig {
    private val secret = Environment[JWT_ENV] ?: error("$JWT_ENV not set in .env")
    val realm = Environment[REALM_ENV] ?: error("$REALM_ENV not set in .env")

    fun makeToken(userId: Int): String = JWT.create()
        .withClaim("userId", userId)
        .sign(Algorithm.HMAC256(secret))

    fun verify() = JWT.require(Algorithm.HMAC256(secret))
        .build()
}