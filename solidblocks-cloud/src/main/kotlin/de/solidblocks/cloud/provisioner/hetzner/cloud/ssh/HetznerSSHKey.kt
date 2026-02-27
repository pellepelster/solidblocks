package de.solidblocks.cloud.provisioner.hetzner.cloud.ssh

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource

class HetznerSSHKey(name: String, val publicKey: String, labels: Map<String, String>) :
    BaseLabeledInfrastructureResource<HetznerSSHKeyRuntime>(name, emptySet(), labels) {

    fun asLookup() = HetznerSSHKeyLookup(name)

    override fun logText() = "SSH key '$name'"
    override val lookupType = HetznerSSHKeyLookup::class
}
