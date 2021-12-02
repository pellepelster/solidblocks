package de.solidblocks.cli

import de.solidblocks.cloud.config.model.CloudConfiguration
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration

object Contants {
    fun pkiMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-pki"

    fun kvMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-kv"

    fun hostSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-user-ssh"
}
