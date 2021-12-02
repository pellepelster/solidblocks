package de.solidblocks.api.resources.infrastructure.ssh

import de.solidblocks.core.IInfrastructureResource

data class SshKey(val id: String, val publicKey: String) :
    ISshKeyLookup,
    IInfrastructureResource<SshKey, SshKeyRuntime> {

    override fun id(): String {
        return this.id
    }
}
