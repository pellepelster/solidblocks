package de.solidblocks.provisioner.hetzner.dns.zone

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import io.pelle.hetzner.HetznerDnsAPI
import mu.KotlinLogging

class HetznerDnsZoneResourceProvisioner(hetznerDnsAPI: HetznerDnsAPI) :
    IResourceLookupProvider<IDnsZoneLookup, DnsZoneRuntime>,
    BaseHetznerProvisioner<DnsZone, DnsZoneRuntime, HetznerDnsAPI>(hetznerDnsAPI) {

    private val logger = KotlinLogging.logger {}

    override fun lookup(lookup: IDnsZoneLookup): Result<DnsZoneRuntime> {
        return checkedApiCall {
            it.searchZone(lookup.id())
        }.mapNonNullResult {
            DnsZoneRuntime(it.id, it.name)
        }
    }

    override fun getLookupType(): Class<*> {
        return IDnsZoneLookup::class.java
    }
}
