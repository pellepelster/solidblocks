package de.solidblocks.service.integrationtest

import com.github.ajalt.clikt.core.CliktCommand
import io.ktor.application.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    @OptIn(KtorExperimentalLocationsAPI::class)
    override fun run() {
        println("Working Directory = " + System.getProperty("user.dir"))

        embeddedServer(Netty, port = 8080) {
            install(Locations) // see https://ktor.io/docs/features-locations.html

            routing {
            }
        }.start(wait = true)
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
