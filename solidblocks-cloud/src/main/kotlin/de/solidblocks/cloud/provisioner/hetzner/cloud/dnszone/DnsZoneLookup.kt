package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.resources.ResourceLookup

data class DnsZoneLookup(override val name: String) : ResourceLookup<DnsZoneRuntime>
