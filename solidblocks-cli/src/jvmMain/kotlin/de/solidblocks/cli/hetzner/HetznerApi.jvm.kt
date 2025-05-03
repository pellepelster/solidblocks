package de.solidblocks.cli.hetzner

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual fun createHttpClient(url: kotlin.String, apiToken: kotlin.String) = io.ktor.client.HttpClient(Java) {
    install(ContentNegotiation) {
        json(Json {
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    defaultRequest {
        url(url)
        headers.append("Authorization", "Bearer $apiToken")
    }
}
