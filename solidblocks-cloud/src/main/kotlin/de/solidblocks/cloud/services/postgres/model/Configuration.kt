package de.solidblocks.cloud.services.postgres.model

import de.solidblocks.cloud.services.ServiceConfiguration

data class PostgresSqlServiceConfiguration(
    override val name: String,
    val databases: List<PostgresSqlServiceDatabaseConfiguration>,
) : ServiceConfiguration {
    override val type = "postgresql"
}

data class PostgresSqlServiceDatabaseConfiguration(val name: String, val users: List<PostgresSqlServiceDatabaseUserConfiguration>)

data class PostgresSqlServiceDatabaseUserConfiguration(val name: String)
