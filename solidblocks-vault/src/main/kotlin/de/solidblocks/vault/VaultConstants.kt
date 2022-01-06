package de.solidblocks.vault

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants.cloudId
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import java.time.Duration

object VaultConstants {

    const val UNSEAL_KEY_PREFIX = "vault-unseal-key"

    const val ROOT_TOKEN_KEY = "vault-root-token"

    const val CONTROLLER_POLICY_NAME = "controller"

    const val BACKUP_POLICY_NAME = "backup"

    const val SERVICE_BASE_POLICY_NAME = "service-base"

    val SERVICE_TOKEN_TTL = Duration.ofDays(7)

    fun pkiMountName(cloudName: String, environmentName: String) = "${cloudId(cloudName, environmentName)}-pki"

    fun pkiMountName(environment: EnvironmentEntity) = pkiMountName(environment.cloud.name, environment.name)

    fun pkiMountName(reference: EnvironmentReference) = pkiMountName(reference.cloud, reference.environment)

    fun domain(environment: EnvironmentEntity) = "${environment.name}.${environment.cloud.rootDomain}"

    fun domain(reference: ServiceReference, rootDomain: String) = "${reference.service}.${reference.environment}.$rootDomain"

    fun domain(reference: EnvironmentReference, rootDomain: String) = "${reference.environment}.$rootDomain"

    fun kvMountName(reference: EnvironmentReference) = "${cloudId(reference)}-kv"

    fun kvMountName(environment: EnvironmentEntity) = kvMountName(environment.reference)

    fun servicePath(service: String) = "solidblocks/services/$service"

    fun hostSshMountName(environment: EnvironmentEntity) = "${cloudId(environment)}-host-ssh"

    fun userSshMountName(environment: EnvironmentEntity) = "${cloudId(environment)}-user-ssh"

    fun vaultAddress(environment: EnvironmentEntity) =
        "https://vault.${domain(environment)}:8200"
}
