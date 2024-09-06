package de.solidblocks.cli.hetzner

import io.ktor.client.*
import io.ktor.client.engine.darwin.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual fun createHttpClient(url: String, apiToken: String) = HttpClient(Darwin) {
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