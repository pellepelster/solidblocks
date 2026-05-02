package de.solidblocks.cloud.services.postgres.model

import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfiguration
import de.solidblocks.cloud.providers.types.secret.SecretProviderConfiguration
import de.solidblocks.cloud.services.BackupConfig
import de.solidblocks.cloud.services.InstanceConfig
import de.solidblocks.cloud.services.ServiceConfiguration
import kotlin.reflect.KClass

data class PostgresSqlServiceConfiguration(override val name: String, val instance: InstanceConfig, val backup: BackupConfig, val databases: List<PostgresSqlServiceDatabaseConfiguration>) :
    ServiceConfiguration {
    override val type = "postgresql"
    override val neededProviders: List<KClass<*>> = listOf(BackupProviderConfiguration::class, SecretProviderConfiguration::class, CloudResourceProviderConfiguration::class)
}

data class PostgresSqlServiceDatabaseConfiguration(val name: String, val users: List<PostgresSqlServiceDatabaseUserConfiguration>)

data class PostgresSqlServiceDatabaseUserConfiguration(val name: String)
