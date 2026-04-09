package de.solidblocks.cloud.services.postgres.model

import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration

data class PostgresSqlServiceConfiguration(override val name: String, val instance: InstanceConfig, val backup: BackupConfig, val databases: List<PostgresSqlServiceDatabaseConfiguration>) :
    ServiceConfiguration {
    override val type = "postgresql"
}

data class PostgresSqlServiceDatabaseConfiguration(val name: String, val users: List<PostgresSqlServiceDatabaseUserConfiguration>)

data class PostgresSqlServiceDatabaseUserConfiguration(val name: String)
