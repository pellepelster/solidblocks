package de.solidblocks.provisioner.vault.policy

import de.solidblocks.core.IInfrastructureResource
import org.springframework.vault.support.Policy

class VaultPolicy(override val name: String, val rules: Set<Policy.Rule>) :
    IVaultPolicyLookup,
    IInfrastructureResource<VaultPolicy, VaultPolicyRuntime>
