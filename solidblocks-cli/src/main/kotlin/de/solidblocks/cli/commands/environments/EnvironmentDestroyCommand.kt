package de.solidblocks.cli.commands.environments

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
import de.solidblocks.cloud.CloudMananger

class EnvironmentDestroyCommand :
    BaseCloudSpringCommand(name = "destroy", help = "destroy environment") {

    val destroyVolumes by option("--destroy-volumes").flag(default = false)

    override fun run() {
        runSpringApplication {
            it.getBean(CloudMananger::class.java).destroy(destroyVolumes)
        }
    }
}
