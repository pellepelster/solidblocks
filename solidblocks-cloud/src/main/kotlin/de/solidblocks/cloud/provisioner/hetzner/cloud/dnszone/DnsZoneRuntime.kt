package de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime

data class DnsZoneRuntime(
    val id: Long,
    val name: String,
    override val labels: Map<String, String> = emptyMap(),
) : LabeledInfrastructureResourceRuntime
