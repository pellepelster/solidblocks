package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup

class HetznerDnsRecordLookup(name: String, val zone: HetznerDnsZoneLookup) :
    InfrastructureResourceLookup<HetznerDnsRecordRuntime>(name, emptySet()) {}
