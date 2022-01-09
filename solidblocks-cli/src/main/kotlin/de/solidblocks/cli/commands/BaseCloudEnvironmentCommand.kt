package de.solidblocks.cli.commands

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.EnvironmentReference

abstract class BaseCloudEnvironmentCommand(
    help: String = "",
    name: String? = null
) :
    BaseCloudDbCommand(name = name, help = help) {

    val cloud: String by option(help = "cloud name").required()

    val environment: String by option(help = "cloud environment").required()

    val reference: EnvironmentReference
        get() = EnvironmentReference(cloud, environment)
}
