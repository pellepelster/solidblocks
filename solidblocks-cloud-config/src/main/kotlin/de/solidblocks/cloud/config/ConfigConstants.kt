package de.solidblocks.cloud.config

import de.solidblocks.base.Constants.LABEL_PREFIX
import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.TenantConfiguration

object ConfigConstants {

    fun cloudId(cloudName: String, environmentName: String) = "$cloudName-$environmentName"

    fun cloudId(reference: EnvironmentReference) = cloudId(reference.cloud, reference.environment)

    fun cloudId(environment: CloudEnvironmentConfiguration) = cloudId(environment.reference)

    fun networkName(environment: CloudEnvironmentConfiguration) = cloudId(environment)

    fun networkName(tenant: TenantConfiguration) = "${tenant.environment.cloud.name}-${cloudId(tenant.environment)}"

    fun defaultLabels(role: Role) = mapOf("$LABEL_PREFIX/role" to role.toString())

    fun defaultLabels(cloud: CloudConfiguration, role: Role) = defaultLabels(role) + mapOf("$LABEL_PREFIX/cloud" to cloud.name)

    fun defaultLabels(environment: CloudEnvironmentConfiguration, role: Role) = mapOf("$LABEL_PREFIX/environment" to environment.name) + defaultLabels(environment.cloud, role)

    fun defaultLabels(tenant: TenantConfiguration, role: Role) = mapOf("$LABEL_PREFIX/tenant" to tenant.name) + defaultLabels(tenant.environment, role)

    const val GITHUB_TOKEN_RO_KEY = "github_token_ro"
    const val GITHUB_USERNAME_KEY = "github_username"

    const val HETZNER_CLOUD_API_TOKEN_RO_KEY = "hetzner_cloud_api_key_ro"
    const val HETZNER_CLOUD_API_TOKEN_RW_KEY = "hetzner_cloud_api_key_rw"

    const val HETZNER_DNS_API_TOKEN_RW_KEY = "hetzner_dns_api_key_rw"

    const val CONSUL_MASTER_TOKEN_KEY = "consul_master_token"
    const val CONSUL_SECRET_KEY = "consul_secret"

    const val SSH_PUBLIC_KEY = "ssh_public_key"
    const val SSH_PRIVATE_KEY = "ssh_private_key"

    const val SSH_IDENTITY_PUBLIC_KEY = "ssh_identity_public_key"
    const val SSH_IDENTITY_PRIVATE_KEY = "ssh_identity_private_key"
}