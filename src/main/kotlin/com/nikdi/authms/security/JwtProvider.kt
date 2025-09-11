package com.nikdi.authms.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.nikdi.authms.Environment
import com.nikdi.authms.database.AuthEntity
import java.util.*

const val JWT_ENV = "JWT_SECRET"
const val ISSUER_ENV = "ISSUER"
const val REALM_ENV = "KTOR_REALM"

object JwtProvider {
    private val secret = Environment[JWT_ENV] ?: error("$JWT_ENV not set in .env!")
    private val issuer = Environment[ISSUER_ENV] ?: error("$ISSUER_ENV not set in .env!")
    val realm = Environment[REALM_ENV] ?: error("$REALM_ENV not set in .env!")

    private const val ACCESS_TOKEN_LIFETIME = 20 * 60 * 1000L
    private const val REFRESH_TOKEN_LIFETIME = 3 * 24 * 60 * 60 * 1000L

    private fun signToken(
        claims: Map<String, String?>,
        expiresInMs: Long
    ): String {
        val builder = JWT.create()
            .withIssuer(issuer)
            .withSubject("Authentication")
            .withExpiresAt(Date(System.currentTimeMillis() + expiresInMs))

        claims.forEach { (key, value) ->
            if (value != null) builder.withClaim(key, value)
        }

        return builder.sign(Algorithm.HMAC256(secret))
    }

    fun makeAccessToken(user: AuthEntity): String =
        signToken(
            claims = mapOf(
                "type" to "access",
                "userId" to user.id.value.toString()
            ),
            expiresInMs = ACCESS_TOKEN_LIFETIME
        )

    fun makeRefreshToken(user: AuthEntity): String =
        signToken(
            claims = mapOf(
                "type" to "refresh",
                "userId" to user.id.value.toString()
            ),
            expiresInMs = REFRESH_TOKEN_LIFETIME
        )

    fun verifier(): JWTVerifier {
        val req = JWT.require(Algorithm.HMAC256(secret))
        req.withIssuer(issuer)
        return req.build()
    }
}