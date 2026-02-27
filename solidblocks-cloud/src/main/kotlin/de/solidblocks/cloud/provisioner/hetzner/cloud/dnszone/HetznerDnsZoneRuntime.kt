package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime

class HetznerDnsZoneRuntime(
    val id: Long,
    val name: String,
    labels: Map<String, String> = emptyMap(),
) : BaseLabeledInfrastructureResourceRuntime(labels)
