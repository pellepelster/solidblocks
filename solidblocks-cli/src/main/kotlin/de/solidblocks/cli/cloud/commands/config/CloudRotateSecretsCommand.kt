package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.self.BaseSpringCommand
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.stereotype.Component

@Component
class CloudRotateSecretsCommand :
        BaseSpringCommand(name = "rotate-secrets", help = "rotate cloud secrets") {

    val cloud: String by option(help = "name of the cloud").required()

    val environment: String by option(help = "cloud environment").required()

    override fun run() {
        runSpringApplication {
            it.getBean(CloudConfigurationManager::class.java).rotateEnvironmentSecrets(cloud, environment)
        }
    }
}
