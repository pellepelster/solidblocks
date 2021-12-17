package de.solidblocks.cli.commands.environments

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.commands.BaseCloudDbCommand
import de.solidblocks.cloud.SolidblocksAppplicationContext
import de.solidblocks.cloud.config.ConfigConstants.GITHUB_TOKEN_RO_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_CLOUD_API_TOKEN_RW_KEY
import de.solidblocks.cloud.config.ConfigConstants.HETZNER_DNS_API_TOKEN_RW_KEY
import de.solidblocks.cloud.config.model.createConfigValue
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

        val result = context.configurationManager.createEnvironment(
            cloud, environment,
            listOf(
                createConfigValue(GITHUB_TOKEN_RO_KEY, githubReadOnlyToken),
                createConfigValue(HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                createConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                createConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
            )
        )

        if (!result) {
            exitProcess(1)
        }
    }
}