package com.nikdi.authms.security

import com.nikdi.authms.classes.AuthUser
import com.nikdi.authms.classes.Error
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import java.util.*

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            JwtProvider.realm
            verifier(JwtProvider.verifier())
            validate { credential ->
                val userIdString = credential.payload.getClaim("userId").asString()
                userIdString?.let { AuthUser(UUID.fromString(it)) }
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    Error("Missing or invalid token.")
                )
            }
        }
    }
}