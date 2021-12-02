package de.solidblocks.cli.cloud.commands

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.self.BaseSpringCommand
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
abstract class BaseCloudSpringCommand(
    help: String = "",
    name: String? = null
) :
    BaseSpringCommand(name = name, help = help) {

    val cloud: String by option(help = "cloud name").required()

    val environment: String by option(help = "cloud environment").required()

    override fun runSpringApplication(
        arguments: Map<String, String>,
        context: (applicationContext: ApplicationContext) -> Unit
    ) {
        super.runSpringApplication(arguments + mapOf("cloud.name" to cloud, "environment.name" to environment), context)
    }
}
