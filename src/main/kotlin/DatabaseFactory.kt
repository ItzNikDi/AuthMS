package com.nikdi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = Environment["DB_URL"] ?: "jdbc:postgresql://localhost:5432/authdb"
            username = Environment["DB_USER"]
            password = Environment["DB_PASSWORD"]
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 3
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(Users)
        }
    }
}