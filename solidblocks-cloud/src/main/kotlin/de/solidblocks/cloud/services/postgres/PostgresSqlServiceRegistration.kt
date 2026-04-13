package de.solidblocks.cloud.services.postgres

import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationFactory
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationRuntime

class PostgresSqlServiceRegistration : ServiceRegistration<PostgresSqlServiceConfiguration, PostgresSqlServiceConfigurationRuntime> {
    override val supportedConfiguration = PostgresSqlServiceConfiguration::class
    override val supportedRuntime = PostgresSqlServiceConfigurationRuntime::class

    override fun createManager() = PostgresSqlServiceManager()

    override fun createFactory() = PostgresSqlServiceConfigurationFactory()

    override val type = "postgresql"
}
