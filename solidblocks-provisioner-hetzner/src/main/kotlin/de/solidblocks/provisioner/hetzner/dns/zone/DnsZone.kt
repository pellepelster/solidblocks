package de.solidblocks.provisioner.hetzner.dns.zone

import de.solidblocks.core.IInfrastructureResource

data class DnsZone(override val name: String) : IDnsZoneLookup, IInfrastructureResource<DnsZone, DnsZoneRuntime>
