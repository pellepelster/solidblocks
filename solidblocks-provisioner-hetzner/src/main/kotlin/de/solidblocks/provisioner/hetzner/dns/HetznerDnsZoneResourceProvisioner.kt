package de.solidblocks.provisioner.hetzner.dns

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.dns.DnsZone
import de.solidblocks.api.resources.dns.DnsZoneRuntime
import de.solidblocks.core.Result
import de.solidblocks.provisioner.hetzner.cloud.BaseHetznerProvisioner
import io.pelle.hetzner.HetznerDnsAPI
import mu.KotlinLogging
import org.springframework.stereotype.Component

@Component
class HetznerDnsZoneResourceProvisioner(val credentialsProvider: HetznerDnsCredentialsProvider) :
    BaseHetznerProvisioner<DnsZone, DnsZoneRuntime, HetznerDnsAPI>(
        { HetznerDnsAPI(credentialsProvider.defaultApiToken()) },
        DnsZone::class.java
    ) {

    private val logger = KotlinLogging.logger {}

    override fun lookup(resource: DnsZone): Result<DnsZoneRuntime> {
        return checkedApiCall(resource, HetznerDnsAPI::searchZone) {
            it.searchZone(resource.name)
        }.mapNonNullResultNullable {
            DnsZoneRuntime(it.id, it.name)
        }
    }

    override fun apply(resource: DnsZone): Result<*> {
        TODO("Not yet implemented")
    }

    override fun destroy(resource: DnsZone): Result<*> {
        TODO("Not yet implemented")
    }

    override fun destroyAll(): Result<*> {
        TODO("Not yet implemented")
    }

    override fun diff(resource: DnsZone): Result<ResourceDiff> {
        TODO("Not yet implemented")
    }
}
