package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime

data class HetznerDnsRecordRuntime(
    val id: String,
    val name: String,
    val zone: String,
    val values: List<String>,
    override val labels: Map<String, String> = emptyMap(),
) : LabeledInfrastructureResourceRuntime
