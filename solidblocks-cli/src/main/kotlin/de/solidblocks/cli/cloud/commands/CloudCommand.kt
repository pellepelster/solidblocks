package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.GlobalConfig
import kotlin.io.path.ExperimentalPathApi

class CloudCommand : CliktCommand(name = "cloud", help = "manage solidblocks cloud instances") {


    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        //val environment: String by option(help = "cloud environment").required()
        //currentContext.findOrSetObject { GlobalConfig(this.environment) }
    }
}
