package de.solidblocks.provisioner.hetzner.cloud.ssh

import de.solidblocks.core.IInfrastructureResource

data class SshKey(override val name: String, val publicKey: String) :
    ISshKeyLookup,
    IInfrastructureResource<SshKey, SshKeyRuntime>
