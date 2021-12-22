package de.solidblocks.cloud.model.model

import java.util.*

data class CloudModel(
    val id: UUID,
    val name: String,
    val rootDomain: String,
    val configValues: List<CloudConfigValue>
) {
    companion object {
        const val ROOT_DOMAIN_KEY = "root_domain"
    }
}
