package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.solidblocks.cli.cloud.commands.config.BaseCloudSpringCommand
import org.springframework.context.ApplicationContext
import kotlin.system.exitProcess

class CloudDestroyCommand :
    BaseCloudSpringCommand(name = "destroy", help = "bootstrap a cloud") {

    val destroyVolumes by option("--destroy-volumes").flag(default = false)

    override fun run(applicationContext: ApplicationContext) {
        applicationContext.getBean(CloudMananger::class.java).destroy(destroyVolumes)
    }
}
