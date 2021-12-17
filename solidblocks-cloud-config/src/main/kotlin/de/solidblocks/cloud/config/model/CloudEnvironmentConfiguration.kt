package de.solidblocks.cloud.config.model

import de.solidblocks.base.EnvironmentReference
import java.util.*

data class CloudEnvironmentConfiguration(
    val id: UUID,
    val name: String,
    val sshSecrets: SshSecrets,
    val configValues: List<CloudConfigValue>,
    val cloud: CloudConfiguration
) {
    fun getConfigValue(key: String) = configValues.getConfigValue(key)!!.value

    val reference: EnvironmentReference
        get() = EnvironmentReference(cloud.name, name)
}
