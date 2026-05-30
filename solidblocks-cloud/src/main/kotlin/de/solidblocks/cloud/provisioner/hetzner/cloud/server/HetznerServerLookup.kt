package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerServerLookup(name: String, dependsOn: Set<BaseResource> = emptySet()) : InfrastructureResourceLookup<HetznerServerRuntime>(name, dependsOn) {
    override fun logText() = "server '$name'"
}
