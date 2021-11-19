package de.solidblocks.cli

import de.solidblocks.cloud.config.CloudConfig
import de.solidblocks.cloud.config.CloudEnvironmentConfig

object Contants {
    val CONTROLLER_POLICY_NAME = "controller"

    val BACKUP_POLICY_NAME = "backup"

    fun pkiMountName(cloud: CloudConfig, environment: CloudEnvironmentConfig) = "${cloud.name}-${environment.name}-pki"

    fun kvMountName(cloud: CloudConfig, environment: CloudEnvironmentConfig) = "${cloud.name}-${environment.name}-kv"

    fun hostSshMountName(cloud: CloudConfig, environment: CloudEnvironmentConfig) = "${cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(cloud: CloudConfig, environment: CloudEnvironmentConfig) = "${cloud.name}-${environment.name}-user-ssh"
}
