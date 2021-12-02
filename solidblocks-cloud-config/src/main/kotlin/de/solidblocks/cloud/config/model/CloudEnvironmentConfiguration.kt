package de.solidblocks.cloud.config.model

import java.util.*

data class CloudEnvironmentConfiguration(
    val id: UUID,
    val name: String,
    val sshSecrets: SshSecrets,
    val configValues: List<CloudConfigValue>,
    val cloud: CloudConfiguration
)
