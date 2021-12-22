package de.solidblocks.cloud.model.model

import de.solidblocks.base.EnvironmentReference
import java.util.*

data class EnvironmentModel(
    val id: UUID,
    val name: String,
    val sshSecrets: SshSecrets,
    val configValues: List<CloudConfigValue>,
    val cloud: CloudModel
) {
    fun getConfigValue(key: String) = configValues.getConfigValue(key)!!.value

    val reference: EnvironmentReference
        get() = EnvironmentReference(cloud.name, name)
}
