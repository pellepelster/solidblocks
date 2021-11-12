package de.solidblocks.cloud.config

import java.util.*

data class CloudEnvironmentConfig(
        val id: UUID,
        val name: String,
        val sshConfig: SshConfig,
        val configValues: List<CloudConfigValue>,
) {
}
