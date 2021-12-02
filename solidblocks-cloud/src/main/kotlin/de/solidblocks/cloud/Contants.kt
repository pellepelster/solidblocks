package de.solidblocks.cloud

import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration

object Contants {
    fun pkiMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-pki"

    fun kvMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-kv"

    fun hostSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-user-ssh"
}
