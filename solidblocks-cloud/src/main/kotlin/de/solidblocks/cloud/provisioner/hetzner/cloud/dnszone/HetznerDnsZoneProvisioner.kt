package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.BaseHetznerProvisioner
import io.github.oshai.kotlinlogging.KotlinLogging

class HetznerDnsZoneProvisioner(hcloudToken: String) :
    BaseHetznerProvisioner(hcloudToken), ResourceLookupProvider<HetznerDnsZoneLookup, HetznerDnsZoneRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun lookup(lookup: HetznerDnsZoneLookup, context: ProvisionerContext): HetznerDnsZoneRuntime? {
    val zone = api.dnsZones.get(lookup.name) ?: return null

    return HetznerDnsZoneRuntime(
        zone.zone.id,
        zone.zone.name,
    )
  }

  override val supportedLookupType = HetznerDnsZoneLookup::class
}
