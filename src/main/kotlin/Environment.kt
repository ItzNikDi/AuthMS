package com.nikdi

import io.github.cdimascio.dotenv.dotenv

object Environment {
    private val dotenv = dotenv {
        directory = System.getProperty("user.dir")
        filename = ".env"
        ignoreIfMissing = true
    }

    operator fun get(key: String, default: String? = null): String? {
        return dotenv[key] ?: default
    }
}