package de.solidblocks.cli.commands.tenants

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
import de.solidblocks.cloud.TenantMananger
import mu.KotlinLogging

class TenantBootstrapCommand : BaseCloudSpringCommand(name = "bootstrap", help = "bootstrap a tenant") {

    private val logger = KotlinLogging.logger {}

    val tenant: String by option(help = "tenant name").required()

    override fun run() {
        runSpringApplication {
            it.getBean(TenantMananger::class.java).let {
                it.bootstrap(cloud, environment, tenant)
            }
        }
    }
}
