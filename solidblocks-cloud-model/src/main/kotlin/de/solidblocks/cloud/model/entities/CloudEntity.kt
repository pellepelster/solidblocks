package de.solidblocks.cloud.model.entities

import de.solidblocks.base.reference.CloudReference
import java.util.*

data class CloudEntity(
    val id: UUID,
    val name: String,
    val rootDomain: String,
    val isDevelopment: Boolean,
    val configValues: List<CloudConfigValue>
) {
    companion object {
        const val DEVELOPMENT_KEY = "development"
    }

    val reference: CloudReference
        get() = CloudReference(name)
}
