package de.solidblocks.cloud

import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.TenantConfiguration

object Contants {

    fun pkiMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-pki"

    fun kvMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-kv"

    fun hostSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-user-ssh"

    fun defaultLabels(role: Role) = mapOf("role" to role.toString())

    fun defaultLabels(cloud: CloudConfiguration, role: Role) = defaultLabels(role) + mapOf("cloud" to cloud.name)

    fun defaultLabels(environment: CloudEnvironmentConfiguration, role: Role) = mapOf("environment" to environment.name) + defaultLabels(environment.cloud, role)

    fun defaultLabels(tenant: TenantConfiguration, role: Role) = mapOf("environment" to tenant.name) + defaultLabels(tenant.environment, role)

}
