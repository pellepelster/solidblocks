package de.solidblocks.provisioner.vault

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.provisioner.vault.kv.VaultKVProvisioner
import de.solidblocks.provisioner.vault.mount.VaultMountProvisioner
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRoleProvisioner
import de.solidblocks.provisioner.vault.policy.VaultPolicyProvisioner
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRoleProvisioner
import org.springframework.vault.core.VaultTemplate

object Vault {

    fun registerProvisioners(
        provisionerRegistry: ProvisionerRegistry,
        vaultTemplateProvider: () -> VaultTemplate,
    ) {

        provisionerRegistry.addProvisioner(
            VaultSshBackendRoleProvisioner(
                vaultTemplateProvider
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            VaultPolicyProvisioner(
                vaultTemplateProvider
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            VaultPkiBackendRoleProvisioner(
                vaultTemplateProvider
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            VaultMountProvisioner(
                vaultTemplateProvider
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            VaultKVProvisioner(
                vaultTemplateProvider
            ) as IInfrastructureResourceProvisioner<Any, Any>
        )
    }
}
