package de.solidblocks.cli.commands.tenants

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.cli.cloud.commands.BaseCloudSpringCommand
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.springframework.stereotype.Component

@Component
class TenantCreateCommand :
    BaseCloudSpringCommand(
        name = "create",
        help = "create a new tenant"
    ) {

    val tenant: String by option(help = "tenant name").required()

    override fun run() {
        runSpringApplication {
            it.getBean(CloudConfigurationManager::class.java).let {
                it.createTenant(tenant, cloud, environment)
            }
        }
    }
}
