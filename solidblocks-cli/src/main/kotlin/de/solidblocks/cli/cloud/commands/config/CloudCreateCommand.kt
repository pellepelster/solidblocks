package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.CliApplication
import de.solidblocks.cli.config.CliApplicationCloudCreate
import de.solidblocks.cli.self.BaseSpringCommand
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class CloudCreateCommand :
    BaseSpringCommand(
        name = "create",
        help = "create a new cloud configuration",
        cliClass = CliApplicationCloudCreate::class.java
    ) {

    val cloud: String by option(help = "name of the cloud").required()

    val domain: String by option(help = "root domain for the cloud").required()

    override fun run(applicationContext: ApplicationContext) {
        if (!applicationContext.getBean(CloudConfigurationManager::class.java).createCloud(
                cloud,
                domain
            )
        ) {
            exitProcess(1)
        }
    }


    override fun extraArgs(): Map<String, String> {
        return mapOf("spring.profiles.active" to "CloudCreate")
    }
}
