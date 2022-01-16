package de.solidblocks.cli.commands.environment

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.cli.commands.BaseCloudDbCommand

abstract class BaseCloudEnvironmentCommand(
    help: String = "",
    name: String? = null
) :
    BaseCloudDbCommand(name = name, help = help) {

    val cloud: String by option(help = "cloud name").required()

    val environment: String by option(help = "cloud environment").required()

    val environmentRef: EnvironmentResource
        get() = EnvironmentResource(cloud, environment)
}
