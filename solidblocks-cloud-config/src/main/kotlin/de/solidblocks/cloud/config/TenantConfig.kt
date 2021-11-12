package de.solidblocks.cloud.config

import java.util.*

data class TenantConfig(
    val id: UUID,

    val name: String,

    val sshConfig: SshConfig,

    val seedConfig: SeedConfig,

    val solidblocksConfig: SolidblocksConfig,

    val configurations: List<CloudConfigValue>

) {
    companion object {
        const val ROOT_DOMAIN = "root_domain"
    }
}
