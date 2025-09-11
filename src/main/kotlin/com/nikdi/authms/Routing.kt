package com.nikdi.authms

import at.favre.lib.crypto.bcrypt.BCrypt
import com.nikdi.authms.classes.*
import com.nikdi.authms.database.AuthEntity
import com.nikdi.authms.database.AuthTable
import com.nikdi.authms.security.JwtProvider
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLException
import java.util.*

fun Application.configureRouting() {
    routing {
        swaggerUI(path = "/docs") // TODO - create Swagger docs
        get("/") {
            call.respond(HttpStatusCode.Created, "Hello from AuthMS!")
        }
        post("/register") {
            val request = call.receive<UserCredentials>()

            if (request.username.isBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Error("Username cannot be empty.")
                )
                return@post
            }

            val hashed = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

            val userId = UUID.randomUUID()

            try {
                transaction {
                    AuthEntity.new(userId) {
                        username = request.username
                        passwordHash = hashed
                    }
                }
                call.respond(
                    HttpStatusCode.Created,
                    Success("Created.")
                )
            } catch (e: Exception) {
                val rootCause = e.cause ?: e
                val psqlException = rootCause as? PSQLException ?: (rootCause.cause as? PSQLException)

                if (psqlException?.sqlState == "23505") {
                    call.respond(
                        HttpStatusCode.Conflict,
                        Error("Username already taken.")
                    )
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Error("Database error.")
                    )
                }
            }
        }

        post("/login") {
            val request = call.receive<UserCredentials>()

            val user = transaction {
                AuthEntity.find { AuthTable.username eq request.username }.singleOrNull()
            } ?: return@post call.respond(HttpStatusCode.NotFound, Error("No user found."))

            val result = BCrypt.verifyer().verify(
                request.password.toCharArray(),
                user.passwordHash
            )

            if (!result.verified) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    Error("Invalid credentials.")
                )
                return@post
            }

            val accessToken = JwtProvider.makeAccessToken(user)
            val refreshToken = JwtProvider.makeRefreshToken(user)

            transaction {
                user.refreshToken = refreshToken
            }

            call.respond(
                HttpStatusCode.OK,
                TokenResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )
            )
        }

        post("/refresh") {
            val request = try {
                call.receive<RefreshToken>()
            } catch (e: Exception) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    Error("Missing refresh token.")
                )
            }

            val refreshToken = request.refreshToken

            try {
                val decoded = JwtProvider.verifier().verify(refreshToken)

                if (decoded.getClaim("type").asString() != "refresh") {
                    call.respond(HttpStatusCode.BadRequest, Error("Not a refresh token."))
                    return@post
                }

                val userId = UUID.fromString(decoded.getClaim("userId").asString())
                val user = transaction { AuthEntity.findById(userId) }
                    ?: return@post call.respond(
                        HttpStatusCode.NotFound,
                        Error("User not found.")
                    )

                if (user.refreshToken != refreshToken) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        Error("Invalid refresh token.")
                    )
                    return@post
                }

                val newAccessToken = JwtProvider.makeAccessToken(user)
                val newRefreshToken = JwtProvider.makeRefreshToken(user)

                transaction {
                    user.refreshToken = newRefreshToken
                }

                call.respond(
                    HttpStatusCode.OK,
                    TokenResponse(
                        accessToken = newAccessToken,
                        refreshToken = newRefreshToken
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    Error("Invalid or expired refresh token.")
                )
            }
        }

        post("/introspect") {
            val request = try {
                call.receive<AccessToken>()
            } catch (e: Exception) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    Error("Missing access token.")
                )
            }

            val accessToken = request.accessToken

            try {
                val decoded = JwtProvider.verifier().verify(accessToken)

                call.respond(
                    HttpStatusCode.OK,
                    TokenIntrospectResponse(
                        active = true,
                        payload = TokenPayload(
                            sub = decoded.subject,
                            userId = decoded.getClaim("userId").asString(),
                            exp = decoded.expiresAt.time / 1000
                        )
                    ),
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.OK,
                    TokenIntrospectResponse(false)
                )
            }
        }

        authenticate("auth-jwt") {
            post("/logout") {
                val authUser = call.principal<AuthUser>()!!

                val userId = transaction { AuthEntity.findById(authUser.userId)?.id } ?: return@post call.respond(
                    HttpStatusCode.NotFound,
                    Error("User not found.")
                )

                try {
                    transaction {
                        AuthEntity.findById(userId)?.apply {
                            refreshToken = null
                        }
                    }

                    call.respond(
                        HttpStatusCode.OK,
                        Success("Logged out.")
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        Error("Couldn't log out.")
                    )
                }
            }
        }
    }
}

