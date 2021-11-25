package de.solidblocks.cli

import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration

object Contants {
    fun pkiMountName(cloud: CloudConfiguration, environment: CloudEnvironmentConfiguration) = "${cloud.name}-${environment.name}-pki"

    fun kvMountName(cloud: CloudConfiguration, environment: CloudEnvironmentConfiguration) = "${cloud.name}-${environment.name}-kv"

    fun hostSshMountName(cloud: CloudConfiguration, environment: CloudEnvironmentConfiguration) = "${cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(cloud: CloudConfiguration, environment: CloudEnvironmentConfiguration) = "${cloud.name}-${environment.name}-user-ssh"
}
