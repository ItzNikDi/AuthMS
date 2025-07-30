package com.nikdi

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLException

fun Application.module() {
    DatabaseFactory.init()
    install(ContentNegotiation) { json() }
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(JwtConfig.verify())
            validate {
                it.payload.getClaim("userId")?.asInt()?.let { id -> AuthUser(id) }
            }
        }
    }

    routing {
        swaggerUI(path = "/docs") // TODO - create Swagger docs
        get("/"){
            call.respond(HttpStatusCode.Created, "Hello from Ktor!")
        }
        post("/register") {
            val req = call.receive<UserCredentials>()
            val hashed = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())

            try {
                val user = transaction {
                    UserEntity.new {
                        username = req.username
                        passwordHash = hashed
                    }
                }
                call.respond(HttpStatusCode.Created, mapOf("token" to JwtConfig.makeToken(user.id.value)))
            } catch (e: Exception) {
                val rootCause = e.cause ?: e
                val psqlException = rootCause as? PSQLException ?: (rootCause.cause as? PSQLException)

                if (psqlException?.sqlState == "23505") {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Username already taken"))
                } else {
                    rootCause.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Database error"))
                }
            }
        }

        post("/login") {
            val req = call.receive<UserCredentials>()

            val user = transaction {
                UserEntity.find { Users.username eq req.username }.singleOrNull()
            }

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No user found."))
                return@post
            }

            val result = BCrypt.verifyer().verify(
                req.password.toCharArray(),
                user.passwordHash
            )

            if(!result.verified) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials."))
                return@post
            }

            call.respond(mapOf("token" to JwtConfig.makeToken(user.id.value)))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<AuthUser>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid token"))
                    return@get
                }

                call.respond(mapOf("userId" to principal.userId))
            }
        }
    }
}