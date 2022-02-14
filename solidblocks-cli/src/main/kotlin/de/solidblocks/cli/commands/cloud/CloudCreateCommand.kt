package de.solidblocks.cli.commands.cloud

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.reference.CloudReference
import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cli.commands.CommandApplicationContext
import de.solidblocks.cloud.clouds.api.CloudCreateRequest
import mu.KotlinLogging
import kotlin.system.exitProcess

class CloudCreateCommand : BaseCloudDbCommand(name = "create", help = "create a new cloud") {

    private val logger = KotlinLogging.logger {}

    val cloud: String by option(help = "name of the cloud").required()

    val domain: String by option(help = "root domain").required()

    val email: String by option(help = "admin email address").required()

    val password: String by option(help = "admin password").required()

    override fun run() {
        val context = CommandApplicationContext(solidblocksDatabaseUrl)

        val reference = CloudReference(cloud)

        val result = context.managers.clouds.createCloud(reference.cloud, CloudCreateRequest(cloud, domain, email, password))

        if (result.hasErrors()) {
            logger.error { "failed to create cloud" }
            exitProcess(0)
        }
    }
}
