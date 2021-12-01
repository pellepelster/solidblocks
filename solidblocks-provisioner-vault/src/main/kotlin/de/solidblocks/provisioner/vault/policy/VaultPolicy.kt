package de.solidblocks.provisioner.vault.policy

import de.solidblocks.core.IInfrastructureResource
import org.springframework.vault.support.Policy

class VaultPolicy(val id: String, val rules: Set<Policy.Rule>) :
        IVaultPolicyLookup,
        IInfrastructureResource<VaultPolicy, VaultPolicyRuntime> {

    override fun id() = id
}
