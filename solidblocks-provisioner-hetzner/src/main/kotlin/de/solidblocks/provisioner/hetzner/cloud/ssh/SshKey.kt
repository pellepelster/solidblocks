package de.solidblocks.provisioner.hetzner.cloud.ssh

import de.solidblocks.core.IInfrastructureResource

data class SshKey(val id: String, val publicKey: String) :
    ISshKeyLookup,
    IInfrastructureResource<SshKey, SshKeyRuntime> {

    override fun id(): String {
        return this.id
    }
}
