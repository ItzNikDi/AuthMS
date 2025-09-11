package com.nikdi.authms.database

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.*

object AuthTable : UUIDTable(name = "auth_users") {
    val username = varchar("username", 50).uniqueIndex()
    val passwordHash = varchar("password_hash", 64)
    val refreshToken = varchar("refresh_token", 512).nullable()
}

class AuthEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<AuthEntity>(AuthTable)

    var username by AuthTable.username
    var passwordHash by AuthTable.passwordHash
    var refreshToken by AuthTable.refreshToken
}