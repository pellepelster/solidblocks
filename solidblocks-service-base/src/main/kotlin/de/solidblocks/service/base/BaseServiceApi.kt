package de.solidblocks.service.base

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class BaseServiceApi {

    init {
        val server = embeddedServer(Netty, port = 8080) {
            install(Locations)
            install(ContentNegotiation) {
                jackson {
                    this.registerModule(kotlinModule())
                }
            }

            routing {
                versionRoutes()
            }
        }
        server.start(wait = true)
    }
}
