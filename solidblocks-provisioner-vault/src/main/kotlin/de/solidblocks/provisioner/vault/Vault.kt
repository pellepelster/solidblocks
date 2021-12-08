package de.solidblocks.provisioner.vault

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.provisioner.vault.Vault.Constants.Companion.vaultAddress
import de.solidblocks.provisioner.vault.kv.VaultKVProvisioner
import de.solidblocks.provisioner.vault.mount.VaultMountProvisioner
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRoleProvisioner
import de.solidblocks.provisioner.vault.policy.VaultPolicyProvisioner
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRoleProvisioner
import org.springframework.vault.core.VaultTemplate

class Vault {

    class Constants {
        companion object {
            val CONTROLLER_POLICY_NAME = "controller"
            val BACKUP_POLICY_NAME = "backup"

            fun vaultAddress(environment: CloudEnvironmentConfiguration) =
                "https://vault.${environment.name}.${environment.cloud.rootDomain}:8200"
        }
    }

    companion object {

        fun vaultTemplateProvider(
            environmentConfiguration: CloudEnvironmentConfiguration,
            configurationManager: CloudConfigurationManager
        ): () -> VaultTemplate {

            return {
                VaultRootClientProvider(
                    environmentConfiguration.cloud.name,
                    environmentConfiguration.name,
                    configurationManager,
                    vaultAddress(environmentConfiguration)
                ).createClient()
            }
        }

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
}
