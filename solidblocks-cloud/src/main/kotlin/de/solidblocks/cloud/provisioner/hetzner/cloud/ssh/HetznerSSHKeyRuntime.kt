package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime

class HetznerSSHKeyRuntime(
    val id: Long,
    val name: String,
    val fingerprint: String,
    val publicKey: String,
    labels: Map<String, String>,
) : BaseLabeledInfrastructureResourceRuntime(labels)
