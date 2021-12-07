package de.solidblocks.cloud.config.model

import java.util.*

data class TenantConfiguration(
    val id: UUID,
    val name: String,
    val environment: CloudEnvironmentConfiguration
)
