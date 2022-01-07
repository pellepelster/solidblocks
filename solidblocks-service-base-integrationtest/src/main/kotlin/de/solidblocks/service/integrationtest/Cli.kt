package de.solidblocks.service.integrationtest

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.service.base.BaseServiceApi
import mu.KotlinLogging

class RunCommand : CliktCommand(name = "run") {

    private val logger = KotlinLogging.logger {}

    override fun run() {
        val service = BaseServiceApi()
    }
}

fun main(args: Array<String>) {
    RunCommand().main(args)
}
