package de.solidblocks.agent.base

import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

class AgentHttpServer {

    val shutdown = CountDownLatch(1)

    val server: NettyApplicationEngine

    init {
        server = embeddedServer(Netty, port = 8080) {
            install(Locations)
            install(ContentNegotiation) {
                jackson {
                    this.registerModule(kotlinModule())
                }
            }

            routing {
                versionRoutes(shutdown)
            }
        }
    }

    fun startAndWait() {
        server.start(false)
        shutdown.await()
        server.stop(4000, 4000)
        exitProcess(7)
    }
}
