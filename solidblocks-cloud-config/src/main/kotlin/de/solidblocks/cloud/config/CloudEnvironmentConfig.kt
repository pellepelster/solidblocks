package de.solidblocks.cloud.config

import java.util.*

data class CloudEnvironmentConfig(
    val id: UUID,
    val name: String,
    val sshSecrets: SshSecrets,
    val configValues: List<CloudConfigValue>,
) {
}
