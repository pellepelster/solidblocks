package de.solidblocks.cli.commands.tenant

import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.solidblocks.base.resources.TenantResource
import de.solidblocks.cli.commands.environment.BaseCloudEnvironmentCommand

abstract class BaseCloudTenantCommand(
    help: String = "",
    name: String? = null
) :
    BaseCloudEnvironmentCommand(name = name, help = help) {

    val tenant: String by option(help = "tenant").required()

    val tenantRef: TenantResource
        get() = TenantResource(cloud, environment, tenant)
}
