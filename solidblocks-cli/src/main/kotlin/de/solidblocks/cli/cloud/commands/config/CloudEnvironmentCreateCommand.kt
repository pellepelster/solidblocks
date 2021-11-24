package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.Constants.ConfigKeys.Companion.GITHUB_TOKEN_RO_KEY
import de.solidblocks.base.Constants.ConfigKeys.Companion.HETZNER_CLOUD_API_TOKEN_RO_KEY
import de.solidblocks.base.Constants.ConfigKeys.Companion.HETZNER_CLOUD_API_TOKEN_RW_KEY
import de.solidblocks.base.Constants.ConfigKeys.Companion.HETZNER_DNS_API_TOKEN_RW_KEY
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.createConfigValue
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi
import kotlin.system.exitProcess


@Component
class CloudEnvironmentCreateCommand :
        CliktCommand(name = "create-environment", help = "create a new cloud environment") {

    val cloud: String by option(help = "name of the cloud").required()

    val environment: String by option(help = "cloud environment").required()

    val hetznerCloudApiTokenReadOnly: String by option(help = "Hetzner Cloud api token (ro)").required()

    val hetznerCloudApiTokenReadWrite: String by option(help = "Hetzner Cloud api token (rw)").required()

    val hetznerDnsApiToken: String by option(help = "Hetzner DNS api token").required()

    val githubReadOnlyToken: String by option(help = "Github read only API token").required()

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalPathApi::class)
    override fun run() {

        logger.error { "creating environment '${environment}' for cloud '$cloud'" }

        SpringContextUtil.bean(CloudConfigurationManager::class.java).let {

            if (!it.hasCloud(cloud)) {
                logger.error { "cloud '$cloud' not found" }
                exitProcess(1)
            }

            if (!it.createEnvironment(cloud, environment, listOf(
                            createConfigValue(GITHUB_TOKEN_RO_KEY, githubReadOnlyToken),
                            createConfigValue(HETZNER_CLOUD_API_TOKEN_RO_KEY, hetznerCloudApiTokenReadOnly),
                            createConfigValue(HETZNER_CLOUD_API_TOKEN_RW_KEY, hetznerCloudApiTokenReadWrite),
                            createConfigValue(HETZNER_DNS_API_TOKEN_RW_KEY, hetznerDnsApiToken),
                    ))) {
                exitProcess(1)
            }
        }
    }
}
