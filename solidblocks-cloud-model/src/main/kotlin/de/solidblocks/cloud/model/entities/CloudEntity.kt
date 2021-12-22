package de.solidblocks.cloud.model.entities

import java.util.*

data class CloudEntity(
    val id: UUID,
    val name: String,
    val rootDomain: String,
    val configValues: List<CloudConfigValue>
) {
    companion object {
        const val ROOT_DOMAIN_KEY = "root_domain"
    }
}
