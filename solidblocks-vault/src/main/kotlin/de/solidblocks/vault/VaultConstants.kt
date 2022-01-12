package de.solidblocks.vault

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.EnvironmentServiceReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.model.ModelConstants.environmentId
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.cloud.model.ModelConstants.tenantId
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import org.springframework.vault.support.Policy
import java.time.Duration

object VaultConstants {

    const val UNSEAL_KEY_PREFIX = "vault-unseal-key"

    const val ROOT_TOKEN_KEY = "vault-root-token"

    val SERVICE_TOKEN_TTL = Duration.ofDays(7)

    val ENVIRONMENT_TOKEN_TTL = SERVICE_TOKEN_TTL

    fun ingressPolicyName(reference: EnvironmentReference) = "${environmentId(reference)}-ingress"

    fun backupPolicyName(reference: EnvironmentReference) = "${environmentId(reference)}-backup"

    fun environentBasePolicyName(reference: EnvironmentReference) = "${environmentId(reference)}-base-service"

    fun tenantBaseServicePolicyName(reference: TenantReference) = "${tenantId(reference)}-base-service"

    fun servicePolicyName(reference: ServiceReference) = serviceId(reference)

    fun pkiMountName(reference: EnvironmentReference) = "${environmentId(reference)}-pki"

    fun pkiMountName(reference: TenantReference) = "${tenantId(reference)}-pki"

    fun domain(reference: ServiceReference, rootDomain: String) =
        "${reference.service}.${reference.tenant}.${reference.environment}.$rootDomain"

    fun domain(reference: EnvironmentReference, rootDomain: String) =
        "${reference.environment}.$rootDomain"

    fun domain(reference: EnvironmentServiceReference, rootDomain: String) =
        "${reference.service}.${reference.environment}.$rootDomain"

    fun kvMountName(reference: EnvironmentReference) = "${environmentId(reference)}-kv"

    fun kvMountName(reference: TenantReference) = "${tenantId(reference)}-kv"

    fun kvMountName(environment: EnvironmentEntity) = kvMountName(environment.reference)

    fun servicePath(service: String) = "solidblocks/services/$service"

    fun hostSshMountName(reference: EnvironmentReference) = "${environmentId(reference)}-host-ssh"

    fun userSshMountName(reference: EnvironmentReference) = "${environmentId(reference)}-user-ssh"

    fun vaultAddress(environment: EnvironmentEntity) =
        "https://vault.${domain(environment.reference, environment.cloud.rootDomain)}:8200"

    fun providersGithubPolicy(reference: EnvironmentReference) = Policy.Rule.builder().path(
        "${kvMountName(reference)}/data/solidblocks/cloud/providers/github"
    ).capabilities(Policy.BuiltinCapabilities.READ).build()

    fun tokenSelfRenewalPolicies() = setOf(
        Policy.Rule.builder().path("/auth/token/renew-self").capabilities(Policy.BuiltinCapabilities.UPDATE)
            .build(),
        Policy.Rule.builder().path("/auth/token/lookup-self").capabilities(Policy.BuiltinCapabilities.READ)
            .build(),
    )
}
