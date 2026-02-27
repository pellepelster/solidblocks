package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource

class HetznerDnsZone(
    name: String,
    labels: Map<String, String> = emptyMap(),
) : BaseLabeledInfrastructureResource<HetznerDnsZoneRuntime>(name, emptySet(), labels) {
    fun asLookup() = HetznerDnsZoneLookup(name)

    override val lookupType = HetznerDnsZoneLookup::class

}
