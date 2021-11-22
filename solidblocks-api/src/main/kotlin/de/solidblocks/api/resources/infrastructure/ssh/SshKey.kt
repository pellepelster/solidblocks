package de.solidblocks.api.resources.infrastructure.ssh

import de.solidblocks.core.IInfrastructureResource

data class SshKey(val name: String, val publicKey: String) : ISshKeyLookup,
        IInfrastructureResource<SshKey, SshKeyRuntime> {

    override fun name(): String {
        return this.name
    }
}
