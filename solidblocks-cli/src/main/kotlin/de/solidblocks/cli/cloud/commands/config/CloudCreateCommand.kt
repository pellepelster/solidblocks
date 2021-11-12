package de.solidblocks.cli.cloud.commands.config

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.config.GlobalConfig
import de.solidblocks.cli.config.SpringContextUtil
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.hetzner.cloud.createHetznerCloudApiToken
import de.solidblocks.provisioner.hetzner.dns.createHetznerDnsApiTokenConfig
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.io.path.ExperimentalPathApi

@Component
class CloudCreateCommand :
    CliktCommand(name = "create", help = "create a new cloud configuration") {

    val name: String by option(help = "name of the cloud").required()

    val domain: String by option(help = "root domain for the cloud").required()

    //val adminEmail: String by option(help = "admin email address").required()

    val hetznerCloudApiToken: String by option(help = "Hetzner Cloud api token").required()

    val hetznerDnsApiToken: String by option(help = "Hetzner DNS api token").required()

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        val environment = currentContext.findObject<GlobalConfig>()!!.environment

        SpringContextUtil.bean(CloudConfigurationManager::class.java).let {
            it.createCloud(
                    name,
                    domain,

                    )
            it.createEnvironment(name, environment, listOf(
                    createHetznerCloudApiToken(hetznerCloudApiToken),
                    createHetznerDnsApiTokenConfig(hetznerDnsApiToken),
            ))
        }
    }
}
