package de.solidblocks.vault

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants.environmentId
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import java.time.Duration

object VaultConstants {

    const val UNSEAL_KEY_PREFIX = "vault-unseal-key"

    const val ROOT_TOKEN_KEY = "vault-root-token"

    const val CONTROLLER_POLICY_NAME = "controller"

    const val BACKUP_POLICY_NAME = "backup"

    const val SERVICE_BASE_POLICY_NAME = "service-base"

    val SERVICE_TOKEN_TTL = Duration.ofDays(7)

    val ENVIRONMENT_TOKEN_TTL = SERVICE_TOKEN_TTL

    fun pkiMountName(reference: EnvironmentReference) = "${environmentId(reference)}-pki"

    fun domain(reference: ServiceReference, rootDomain: String) =
        "${reference.service}.${reference.environment}.$rootDomain"

    fun kvMountName(reference: EnvironmentReference) = "${environmentId(reference)}-kv"

    fun kvMountName(environment: EnvironmentEntity) = kvMountName(environment.reference)

    fun servicePath(service: String) = "solidblocks/services/$service"

    fun hostSshMountName(reference: EnvironmentReference) = "${environmentId(reference)}-host-ssh"

    fun userSshMountName(reference: EnvironmentReference) = "${environmentId(reference)}-user-ssh"

    fun domain(environment: EnvironmentEntity) = "${environment.name}.${environment.cloud.rootDomain}"

    fun vaultAddress(environment: EnvironmentEntity) =
        "https://vault.${domain(environment)}:8200"
}
