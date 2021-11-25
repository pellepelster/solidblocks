package de.solidblocks.cloud.config.model

import java.util.*

data class CloudConfiguration(
    val id: UUID,
    val name: String,
    val rootDomain: String,
    val configValues: List<CloudConfigValue>,
    val environments: List<CloudEnvironmentConfiguration>
        ) {
    companion object {
        const val ROOT_DOMAIN_KEY = "root_domain"
    }
}
