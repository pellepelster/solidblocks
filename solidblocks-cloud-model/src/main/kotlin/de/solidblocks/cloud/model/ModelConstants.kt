package de.solidblocks.cloud.model

import de.solidblocks.base.BaseConstants.environmentHostFQDN
import de.solidblocks.base.BaseConstants.environmentId
import de.solidblocks.base.BaseConstants.serverName
import de.solidblocks.base.BaseConstants.serviceId
import de.solidblocks.base.BaseConstants.tenantId
import de.solidblocks.base.resources.CloudResource
import de.solidblocks.base.resources.EnvironmentResource
import de.solidblocks.base.resources.ServiceResource
import de.solidblocks.base.resources.TenantResource
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.NodeRole

object ModelConstants {

    const val LABEL_PREFIX = "solidblocks"

    val SERVICE_LABEL: String = "$LABEL_PREFIX/service"

    val ROLE_LABEL: String = "$LABEL_PREFIX/role"

    fun serviceBucketName(reference: ServiceResource) = serviceId(reference)

    fun serviceConfigPath(reference: ServiceResource) =
        "/solidblocks/services/${reference.service}/config"

    fun vaultAddress(environment: EnvironmentEntity) =
        "https://${environmentHostFQDN("vault", environment.reference, environment.cloud.rootDomain)}:8200"

    fun networkName(environment: EnvironmentResource) = environmentId(environment)

    fun sshKeyName(environment: EnvironmentResource) = environmentId(environment)

    fun networkName(reference: TenantResource) = tenantId(reference)

    fun defaultRoleLabels(nodeRole: NodeRole) = mapOf(ROLE_LABEL to nodeRole.toString())

    fun defaultCloudLabels(reference: CloudResource) =
        mapOf("$LABEL_PREFIX/cloud" to reference.cloud)

    fun defaultEnvironmentLabels(reference: EnvironmentResource) =
        mapOf("$LABEL_PREFIX/environment" to reference.environment) + defaultCloudLabels(reference)

    fun defaultCloudLabels(cloud: CloudResource, nodeRole: NodeRole) =
        defaultRoleLabels(nodeRole) + defaultCloudLabels(cloud)

    fun defaultEnvironmentLabels(reference: EnvironmentResource, nodeRole: NodeRole) =
        mapOf("$LABEL_PREFIX/environment" to reference.environment) + defaultCloudLabels(reference, nodeRole)

    fun defaultTenantLabels(reference: TenantResource) =
        mapOf("$LABEL_PREFIX/tenant" to reference.tenant) + defaultEnvironmentLabels(reference)

    fun defaultTenantLabels(reference: TenantResource, nodeRole: NodeRole) =
        defaultTenantLabels(reference) + defaultRoleLabels(nodeRole)

    fun defaultServiceLabels(reference: ServiceResource, nodeRole: NodeRole) =
        mapOf(SERVICE_LABEL to reference.service) + defaultTenantLabels(reference) + defaultRoleLabels(nodeRole)

    fun volumeName(reference: ServiceResource, location: String, index: Int) =
        serverName(reference, location, index)

    fun volumeName(name: String, reference: EnvironmentResource, location: String, index: Int) =
        serverName(name, reference, location, index)

    fun floatingIpName(name: String, reference: EnvironmentResource, location: String, index: Int) =
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
