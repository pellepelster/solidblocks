package de.solidblocks.cli.commands.environments

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.CloudReference
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import kotlin.system.exitProcess

class EnvironmentCreateCommand :
    BaseCloudDbCommand(name = "create", help = "create new environment") {

    val cloud: String by option(help = "name of the cloud").required()

    val environment: String by option(help = "cloud environment").required()

    val hetznerCloudApiTokenReadOnly: String by option(help = "Hetzner Cloud api token (ro)").required()

    val hetznerCloudApiTokenReadWrite: String by option(help = "Hetzner Cloud api token (rw)").required()

    val hetznerDnsApiToken: String by option(help = "Hetzner DNS api token").required()

    val githubReadOnlyToken: String by option(help = "Github read only API token").required()

    override fun run() {
        val context = SolidblocksAppplicationContext(solidblocksDatabaseUrl)

        val reference = CloudReference(cloud)
        if (!context.verifyReference(reference)) {
            exitProcess(1)
        }

        val result = context.cloudManager.createEnvironment(
            reference = EnvironmentReference(cloud, environment),
            githubReadOnlyToken,
            hetznerCloudApiTokenReadOnly,
            hetznerCloudApiTokenReadWrite,
            hetznerDnsApiToken
        )

        if (!result) {
            exitProcess(1)
        }
    }
}
