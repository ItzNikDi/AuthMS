package com.nikdi.authms.database

import com.nikdi.authms.Environment
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun configureDatabase() {
    val config = HikariConfig().apply {
        jdbcUrl = Environment["DB_URL"] ?: "jdbc:postgresql://auth-db:5432/authmsdb"
        username = Environment["POSTGRES_USER"] ?: error("POSTGRES_USER not set in .env")
        password = Environment["POSTGRES_PASSWORD"] ?: error("POSTGRES_PASSWORD not set in .env")
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 3
    }

    val dataSource = HikariDataSource(config)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(AuthTable)
    }
}