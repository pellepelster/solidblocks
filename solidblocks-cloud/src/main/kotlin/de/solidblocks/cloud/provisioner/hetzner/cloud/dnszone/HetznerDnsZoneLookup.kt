package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class HetznerDnsZoneLookup(name: String) : InfrastructureResourceLookup<HetznerDnsZoneRuntime>(name, emptySet())
