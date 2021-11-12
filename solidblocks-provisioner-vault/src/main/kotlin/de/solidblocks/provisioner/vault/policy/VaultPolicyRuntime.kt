package de.solidblocks.provisioner.vault.policy

import org.springframework.vault.support.Policy

data class VaultPolicyRuntime(val rules: Set<Policy.Rule>)
