package de.solidblocks.cloud.services.postgres.model

import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.ServiceCommonConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class PostgresSqlServiceConfiguration(
    override val common: ServiceCommonConfig,
    val backup: BackupConfig,
    val databases: List<PostgresSqlServiceDatabaseConfiguration>,
    val majorVersion: Int,
) :
    ServiceConfiguration {
    override val type = "postgresql"
}

data class PostgresSqlServiceDatabaseConfiguration(val name: String, val users: List<PostgresSqlServiceDatabaseUserConfiguration>)

data class PostgresSqlServiceDatabaseUserConfiguration(val name: String, val admin: Boolean, val read: Boolean, val write: Boolean)
