package de.solidblocks.cloud.model

import de.solidblocks.base.Constants.LABEL_PREFIX
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.entities.CloudEntity
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.cloud.model.entities.Role
import de.solidblocks.cloud.model.entities.TenantEntity

object ModelConstants {

    fun serviceId(reference: ServiceReference) =
        "${cloudId(reference.cloud, reference.environment)}-${reference.service}"

    fun serviceBucketName(reference: ServiceReference) = serviceId(reference)

    fun serviceConfigPath(reference: ServiceReference) =
        "/solidblocks/services/${reference.service}/config"

    fun cloudId(cloudName: String, environmentName: String) = "$cloudName-$environmentName"

    fun cloudId(reference: EnvironmentReference) = cloudId(reference.cloud, reference.environment)

    fun cloudId(environment: EnvironmentEntity) = cloudId(environment.reference)

    fun networkName(environment: EnvironmentEntity) = cloudId(environment)

    fun networkName(tenant: TenantEntity) = "${tenant.environment.cloud.name}-${cloudId(tenant.environment)}"

    fun defaultLabels(role: Role) = mapOf("$LABEL_PREFIX/role" to role.toString())

    fun defaultLabels(cloud: CloudEntity) =
        mapOf("$LABEL_PREFIX/cloud" to cloud.name)

    fun defaultLabels(environment: EnvironmentEntity) =
        mapOf("$LABEL_PREFIX/environment" to environment.name) + defaultLabels(environment.cloud)

    fun defaultLabels(cloud: CloudEntity, role: Role) =
        defaultLabels(role) + defaultLabels(cloud)

    fun defaultLabels(environment: EnvironmentEntity, role: Role) =
        mapOf("$LABEL_PREFIX/environment" to environment.name) + defaultLabels(environment.cloud, role)

    fun defaultLabels(tenant: TenantEntity) =
        mapOf("$LABEL_PREFIX/tenant" to tenant.name) + defaultLabels(tenant.environment)

    fun defaultLabels(tenant: TenantEntity, role: Role) =
        defaultLabels(tenant) + defaultLabels(role)

    const val GITHUB_TOKEN_RO_KEY = "github_token_ro"
    const val GITHUB_USERNAME_KEY = "github_username"

    const val CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
    const val CONSUL_SECRET_KEY = "consul_secret"

    const val SSH_PUBLIC_KEY = "ssh_public_key"
    const val SSH_PRIVATE_KEY = "ssh_private_key"

    const val SSH_IDENTITY_PUBLIC_KEY = "ssh_identity_public_key"
    const val SSH_IDENTITY_PRIVATE_KEY = "ssh_identity_private_key"
}
