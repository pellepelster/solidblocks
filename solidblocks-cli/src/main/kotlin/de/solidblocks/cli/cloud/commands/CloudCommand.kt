package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.CliApplication
import mu.KotlinLogging
import org.springframework.boot.SpringApplication
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.system.exitProcess

class CloudCommand : CliktCommand(name = "cloud", help = "manage solidblocks cloud instances") {


    @OptIn(ExperimentalPathApi::class)
    override fun run() {
    }
}
