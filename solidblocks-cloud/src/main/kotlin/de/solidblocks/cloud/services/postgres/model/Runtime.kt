package de.solidblocks.cloud.services.postgres.model

import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class PostgresSqlServiceDatabaseConfigurationRuntime(
    val name: String,
    val users: List<PostgresSqlServiceDatabaseUsersConfigurationRuntime>,
)

data class PostgresSqlServiceDatabaseUsersConfigurationRuntime(val name: String)

data class PostgresSqlServiceConfigurationRuntime(
    override val index: Int,
    override val name: String,
    val instanceSize: Int,
    val backupRetention: Int,
    val buckets: List<PostgresSqlServiceDatabaseConfigurationRuntime>,
) : ServiceConfigurationRuntime
