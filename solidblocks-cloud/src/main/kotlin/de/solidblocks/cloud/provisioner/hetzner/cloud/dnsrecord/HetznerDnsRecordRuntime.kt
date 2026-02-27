package de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime

class HetznerDnsRecordRuntime(
    val id: String,
    val name: String,
    val zone: String,
    val values: List<String>,
    labels: Map<String, String> = emptyMap(),
) : BaseLabeledInfrastructureResourceRuntime(labels)
