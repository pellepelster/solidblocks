package de.solidblocks.cli.commands.environment

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.reference.CloudReference
import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cloud.ApplicationContext
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
        val context = ApplicationContext(solidblocksDatabaseUrl)

        val reference = CloudReference(cloud)
        if (!context.verifyCloudReference(reference)) {
            exitProcess(1)
        }

        context.environmentsManager.create(
            reference = EnvironmentReference(cloud, environment),
            environment,
            "juerge@test.local",
            "password1",
            githubReadOnlyToken,
            hetznerCloudApiTokenReadOnly,
            hetznerCloudApiTokenReadWrite,
            hetznerDnsApiToken
        ) ?: exitProcess(1)
    }
}
