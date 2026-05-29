package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.api.ListableResourceLookupProvider
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner

class HetznerDnsZoneProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken),
    InfrastructureResourceLookupProvider<HetznerDnsZoneLookup, HetznerDnsZoneRuntime>,
    ListableResourceLookupProvider<HetznerDnsZoneLookup> {

    override suspend fun lookup(lookup: HetznerDnsZoneLookup, context: SSHProvisionerContext): HetznerDnsZoneRuntime? {
        val zone = api.dnsZones.get(lookup.name) ?: return null

        return HetznerDnsZoneRuntime(
            zone.zone.id,
            zone.zone.name,
        )
    }

    override suspend fun list() = api.dnsZones.list().map { HetznerDnsZoneLookup(it.name) }

    override val supportedLookupType = HetznerDnsZoneLookup::class
}
