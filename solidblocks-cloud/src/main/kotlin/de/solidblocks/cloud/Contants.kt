package de.solidblocks.cloud

import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration

object Contants {

    val SOLIDBLOCKS_NETWORK = "10.1.0.0/16"

    fun pkiMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-pki"

    fun kvMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-kv"

    fun hostSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-host-ssh"

    fun userSshMountName(environment: CloudEnvironmentConfiguration) = "${environment.cloud.name}-${environment.name}-user-ssh"
}
