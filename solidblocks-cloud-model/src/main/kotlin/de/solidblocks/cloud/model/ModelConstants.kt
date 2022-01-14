package de.solidblocks.cloud.model

import de.solidblocks.base.BaseConstants.environmentHostFQDN
import de.solidblocks.base.BaseConstants.environmentId
import de.solidblocks.base.BaseConstants.serverName
import de.solidblocks.base.BaseConstants.serviceId
import de.solidblocks.base.BaseConstants.tenantId
import de.solidblocks.base.CloudReference
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.base.TenantReference
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.NodeRole

object ModelConstants {

    const val LABEL_PREFIX = "solidblocks"

    val SERVICE_LABEL: String = "$LABEL_PREFIX/service"

    val ROLE_LABEL: String = "$LABEL_PREFIX/role"

    fun serviceBucketName(reference: ServiceReference) = serviceId(reference)

    fun serviceConfigPath(reference: ServiceReference) =
        "/solidblocks/services/${reference.service}/config"

    fun vaultAddress(environment: EnvironmentEntity) =
        "https://${environmentHostFQDN("vault", environment.reference, environment.cloud.rootDomain)}:8200"

    fun networkName(environment: EnvironmentReference) = environmentId(environment)

    fun sshKeyName(environment: EnvironmentReference) = environmentId(environment)

    fun networkName(reference: TenantReference) = tenantId(reference)

    fun defaultRoleLabels(nodeRole: NodeRole) = mapOf(ROLE_LABEL to nodeRole.toString())

    fun defaultCloudLabels(reference: CloudReference) =
        mapOf("$LABEL_PREFIX/cloud" to reference.cloud)

    fun defaultEnvironmentLabels(reference: EnvironmentReference) =
        mapOf("$LABEL_PREFIX/environment" to reference.environment) + defaultCloudLabels(reference)

    fun defaultCloudLabels(cloud: CloudReference, nodeRole: NodeRole) =
        defaultRoleLabels(nodeRole) + defaultCloudLabels(cloud)

    fun defaultEnvironmentLabels(reference: EnvironmentReference, nodeRole: NodeRole) =
        mapOf("$LABEL_PREFIX/environment" to reference.environment) + defaultCloudLabels(reference, nodeRole)

    fun defaultTenantLabels(reference: TenantReference) =
        mapOf("$LABEL_PREFIX/tenant" to reference.tenant) + defaultEnvironmentLabels(reference)

    fun defaultTenantLabels(reference: TenantReference, nodeRole: NodeRole) =
        defaultTenantLabels(reference) + defaultRoleLabels(nodeRole)

    fun defaultServiceLabels(reference: ServiceReference, nodeRole: NodeRole) =
        mapOf(SERVICE_LABEL to reference.service) + defaultTenantLabels(reference) + defaultRoleLabels(nodeRole)

    fun volumeName(reference: ServiceReference, location: String, index: Int) =
        serverName(reference, location, index)

    fun volumeName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun floatingIpName(name: String, reference: EnvironmentReference, location: String, index: Int) =
        serverName(name, reference, location, index)

    const val GITHUB_TOKEN_RO_KEY = "github_token_ro"
    const val GITHUB_USERNAME_KEY = "github_username"

    const val CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
    const val CONSUL_SECRET_KEY = "consul_secret"

    const val SSH_PUBLIC_KEY = "ssh_public_key"
    const val SSH_PRIVATE_KEY = "ssh_private_key"

    const val SSH_IDENTITY_PUBLIC_KEY = "ssh_identity_public_key"
    const val SSH_IDENTITY_PRIVATE_KEY = "ssh_identity_private_key"

    const val TENANT_NETWORK_CIDR_KEY = "network_cidr"
}
