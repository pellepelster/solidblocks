package de.solidblocks.vault

import de.solidblocks.base.BaseConstants.environmentId
import de.solidblocks.base.BaseConstants.serviceId
import de.solidblocks.base.BaseConstants.tenantId
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.TenantReference
import org.springframework.vault.support.Policy
import java.time.Duration

object VaultConstants {

    const val UNSEAL_KEY_PREFIX = "vault-unseal-key"

    val SERVICE_TOKEN_TTL = Duration.ofDays(7)

    val ENVIRONMENT_TOKEN_TTL = SERVICE_TOKEN_TTL

    fun ingressPolicyName(reference: EnvironmentReference) = "${environmentId(reference)}-ingress"

    fun backupPolicyName(reference: EnvironmentReference) = "${environmentId(reference)}-backup"

    fun servicePolicyName(reference: ServiceReference) = serviceId(reference)

    fun environmentServerPkiMountName(reference: EnvironmentReference) = "${environmentId(reference)}-pki-server"

    fun environmentClientPkiMountName(reference: EnvironmentReference) = "${environmentId(reference)}-pki-client"

    fun tenantServerPkiMountName(reference: TenantReference) = "${tenantId(reference)}-pki-server"

    fun tenantClientPkiMountName(reference: TenantReference) = "${tenantId(reference)}-pki-client"

    fun clientsDomain() = "clients"

    fun clientFQDN(client: String) = "$client.${clientsDomain()}"

    fun kvMountName(reference: EnvironmentReference) = "${environmentId(reference)}-kv"

    fun kvMountName(reference: TenantReference) = "${tenantId(reference)}-kv"

    fun servicePath(service: String) = "solidblocks/services/$service"

    fun hostSshMountName(reference: EnvironmentReference) = "${environmentId(reference)}-ssh-host"

    fun userSshMountName(reference: EnvironmentReference) = "${environmentId(reference)}-ssh-user"

    fun providersGithubPolicy(reference: EnvironmentReference) = Policy.Rule.builder().path(
        "${kvMountName(reference)}/data/solidblocks/cloud/providers/github"
    ).capabilities(Policy.BuiltinCapabilities.READ).build()

    fun tokenSelfRenewalPolicy() =
        Policy.Rule.builder().path("/auth/token/renew-self").capabilities(Policy.BuiltinCapabilities.UPDATE)
            .build()

    fun tokenSelfLookupPolicy() =
        Policy.Rule.builder().path("/auth/token/lookup-self").capabilities(Policy.BuiltinCapabilities.READ)
            .build()

    fun issueEnvironmentServerCertificatesPolicy(reference: EnvironmentReference) =
        Policy.Rule.builder()
            .path("${environmentServerPkiMountName(reference)}/issue/${environmentServerPkiMountName(reference)}")
            .capabilities(Policy.BuiltinCapabilities.UPDATE).build()

    fun issueTenantServerCertificatesPolicy(reference: TenantReference) =
        Policy.Rule.builder()
            .path("${tenantServerPkiMountName(reference)}/issue/${tenantServerPkiMountName(reference)}")
            .capabilities(Policy.BuiltinCapabilities.UPDATE).build()

    fun readEnvironmentClientCaCertificatePolicy(reference: TenantReference) =
        Policy.Rule.builder()
            .path("${environmentClientPkiMountName(reference)}/cert/ca")
            .capabilities(Policy.BuiltinCapabilities.READ).build()
}
