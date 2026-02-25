package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource

class HetznerSSHKey(override val name: String, val publicKey: String, labels: Map<String, String>) :
    LabeledInfrastructureResource<HetznerSSHKeyRuntime>(labels) {

    fun asLookup() = HetznerSSHKeyLookup(name)

    override fun logText() = "SSH key '$name'"
}
