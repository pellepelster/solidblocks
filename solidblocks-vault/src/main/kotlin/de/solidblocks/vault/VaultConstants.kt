package de.solidblocks.vault

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cloud.config.ConfigConstants.cloudId
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration

object VaultConstants {

    const val UNSEAL_KEY_PREFIX = "vault-unseal-key"

    const val ROOT_TOKEN_KEY = "vault-root-token"

    const val CONTROLLER_POLICY_NAME = "controller"

    const val BACKUP_POLICY_NAME = "backup"

    fun pkiMountName(cloudName: String, environmentName: String) = "${cloudId(cloudName, environmentName)}-pki"

    fun pkiMountName(environment: CloudEnvironmentConfiguration) = pkiMountName(environment.cloud.name, environment.name)

    fun kvMountName(reference: EnvironmentReference) = "${cloudId(reference)}-kv"

    fun kvMountName(environment: CloudEnvironmentConfiguration) = kvMountName(environment.reference)

    fun servicePath(service: String) = "solidblocks/services/$service"

    fun hostSshMountName(environment: CloudEnvironmentConfiguration) = "${cloudId(environment)}-host-ssh"

    fun userSshMountName(environment: CloudEnvironmentConfiguration) = "${cloudId(environment)}-user-ssh"

    fun vaultAddress(environment: CloudEnvironmentConfiguration) =
        "https://vault.${environment.name}.${environment.cloud.rootDomain}:8200"
}
