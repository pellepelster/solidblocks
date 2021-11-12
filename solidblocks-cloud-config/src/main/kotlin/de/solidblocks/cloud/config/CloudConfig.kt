package de.solidblocks.cloud.config

import java.util.*

data class CloudConfig(
        val id: UUID,
        val name: String,
        val rootDomain: String,
        val configValues: List<CloudConfigValue>,
        val environments: List<CloudEnvironmentConfig>
        ) {
    companion object {
        const val ROOT_DOMAIN_KEY = "root_domain"
    }
}
