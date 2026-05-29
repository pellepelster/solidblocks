package de.solidblocks.cloud.provisioner.garagefs.accesskey

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime

class GarageFsAccessKey(name: String, val server: HetznerServer, val adminToken: GenericSecret<GenericSecretRuntime>, dependsOn: Set<BaseResource> = emptySet()) :
    BaseInfrastructureResource<GarageFsPermissionRuntime>(name, dependsOn + setOf(server)) {
    override fun asLookup() = GarageFsAccessKeyLookup(name, server.asLookup(), adminToken)

    override fun logText() = "GarageFS access key '$name' on ${server.logText()}"

    override val lookupType = GarageFsAccessKeyLookup::class
}
