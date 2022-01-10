package de.solidblocks.ingress.agent

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

        embeddedServer(Netty, port = 8080) {
            install(Locations)
            routing {
            }
        }.start(wait = true)

        /*
        val reference = ServiceReference("xxx", "yyy", "service1")
        val service = VaultServiceManager(reference, "/storage/local", VaultManager("xx", "xx", reference.environmentReference))
        service.start()
        */
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
