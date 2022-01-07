package de.solidblocks.vault.agent

import com.github.ajalt.clikt.core.CliktCommand
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    override fun run() {

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
