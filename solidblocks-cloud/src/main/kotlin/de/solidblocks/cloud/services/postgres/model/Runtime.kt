package de.solidblocks.cloud.services.postgres.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.ServiceCommonRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class PostgresSqlServiceDatabaseConfigurationRuntime(val name: String, val users: List<PostgresSqlServiceDatabaseUsersConfigurationRuntime>)

data class PostgresSqlServiceDatabaseUsersConfigurationRuntime(val name: String, val admin: Boolean, val read: Boolean, val write: Boolean)

data class PostgresSqlServiceConfigurationRuntime(
    override val index: Int,
    override val common: ServiceCommonRuntime,
    override val backup: BackupRuntime,
    val databases: List<PostgresSqlServiceDatabaseConfigurationRuntime>,
    val majorVersion: Int,
) : ServiceConfigurationRuntime
