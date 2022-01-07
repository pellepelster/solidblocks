package de.solidblocks.helloworld.agent

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.agent.base.AgentHttpServer
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        val agentHttpServer = AgentHttpServer()
        agentHttpServer.startAndWait()
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
