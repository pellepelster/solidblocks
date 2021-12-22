package de.solidblocks.cloud.model.entities

import de.solidblocks.base.EnvironmentReference
import java.util.*

data class EnvironmentEntity(
    val id: UUID,
    val name: String,
    val sshSecrets: SshSecrets,
    val configValues: List<CloudConfigValue>,
    val cloud: CloudEntity
) {
    fun getConfigValue(key: String) = configValues.getConfigValue(key)!!.value

    val reference: EnvironmentReference
        get() = EnvironmentReference(cloud.name, name)
}
