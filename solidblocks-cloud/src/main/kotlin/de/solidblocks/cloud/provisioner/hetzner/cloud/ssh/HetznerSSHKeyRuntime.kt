package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime

data class HetznerSSHKeyRuntime(
    val id: Long,
    val name: String,
    val fingerprint: String,
    val publicKey: String,
    override val labels: Map<String, String>,
) : LabeledInfrastructureResourceRuntime
