package de.solidblocks.cloud

import de.solidblocks.base.Constants.LABEL_PREFIX
import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.cloud.config.model.TenantConfiguration

object Contants {

    fun pkiMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-pki"

    fun kvMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-kv"

    fun hostSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-user-ssh"

    fun networkName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}"

    fun networkName(tenant: TenantConfiguration) = "${tenant.environment.cloud.name}-${tenant.environment.name}-${tenant.name}"

    fun defaultLabels(role: Role) = mapOf("$LABEL_PREFIX/role" to role.toString())

    fun defaultLabels(cloud: CloudConfiguration, role: Role) = defaultLabels(role) + mapOf("$LABEL_PREFIX/cloud" to cloud.name)

    fun defaultLabels(environment: CloudEnvironmentConfiguration, role: Role) = mapOf("$LABEL_PREFIX/environment" to environment.name) + defaultLabels(environment.cloud, role)

    fun defaultLabels(tenant: TenantConfiguration, role: Role) = mapOf("$LABEL_PREFIX/tenant" to tenant.name) + defaultLabels(tenant.environment, role)
}
