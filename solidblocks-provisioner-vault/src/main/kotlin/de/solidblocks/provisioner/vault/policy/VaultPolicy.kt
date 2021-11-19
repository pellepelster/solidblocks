package de.solidblocks.provisioner.vault.policy

import de.solidblocks.core.IInfrastructureResource
import org.springframework.vault.support.Policy

class VaultPolicy(val name: String, val rules: Set<Policy.Rule>) :
        IInfrastructureResource<VaultPolicy, VaultPolicyRuntime> {

    override fun name(): String {
        return this.name
    }
}
