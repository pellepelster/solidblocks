package de.solidblocks.provisioner.hetzner.dns

import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.dns.DnsZoneRuntime
import de.solidblocks.api.resources.dns.IDnsZoneLookup
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import io.pelle.hetzner.HetznerDnsAPI
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerDnsZoneResourceProvisioner(val credentialsProvider: HetznerDnsCredentialsProvider) :
        IResourceLookupProvider<IDnsZoneLookup, DnsZoneRuntime>,
        BaseHetznerProvisioner<DnsZone, DnsZoneRuntime, HetznerDnsAPI>(
                { HetznerDnsAPI(credentialsProvider.defaultApiToken()) }) {

    private val logger = KotlinLogging.logger {}

    override fun lookup(lookup: IDnsZoneLookup): Result<DnsZoneRuntime> {
        return checkedApiCall(lookup, HetznerDnsAPI::searchZone) {
            it.searchZone(lookup.name())
        }.mapNonNullResultNullable {
            DnsZoneRuntime(it.id, it.name)
        }
    }

    override fun getLookupType(): Class<*> {
        return IDnsZoneLookup::class.java
    }
}