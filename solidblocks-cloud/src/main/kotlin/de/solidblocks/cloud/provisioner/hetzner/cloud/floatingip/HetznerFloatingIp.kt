package de.solidblocks.cloud.provisioner.hetzner.cloud.floatingip

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.resources.FloatingIpType

class HetznerFloatingIp(
    name: String,
    val type: FloatingIpType,
    val homeLocation: HetznerLocation,
    labels: Map<String, String> = emptyMap(),
    val protected: Boolean = true,
) : BaseLabeledInfrastructureResource<HetznerFloatingIpRuntime>(name, emptySet(), labels) {

    override fun asLookup() = HetznerFloatingIpLookup(name)

    override fun logText() = "floating IP '$name'"

    override val lookupType = HetznerFloatingIpLookup::class
}
