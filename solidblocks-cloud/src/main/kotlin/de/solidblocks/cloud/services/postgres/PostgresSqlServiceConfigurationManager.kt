package de.solidblocks.cloud.services.postgres

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseUsersConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext

class PostgresSqlServiceConfigurationManager(val cloudConfiguration: CloudConfigurationRuntime) :
    ServiceConfigurationManager<PostgresSqlServiceConfiguration, PostgresSqlServiceConfigurationRuntime> {

    override fun createResources(
        runtime: PostgresSqlServiceConfigurationRuntime
    ): List<BaseInfrastructureResource<*>> {
        return emptyList()
    }

    override fun createProvisioners(runtime: PostgresSqlServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>(GarageFsBucketProvisioner(), GarageFsAccessKeyProvisioner(), GarageFsPermissionProvisioner())

    override fun validatConfiguration(
        configuration: PostgresSqlServiceConfiguration,
        context: LogContext,
    ): Result<PostgresSqlServiceConfigurationRuntime> {

        configuration.databases.forEach { database ->
            if (configuration.databases.count { database.name == it.name } > 1) {
                return Error("duplicated database with name '${database.name}', ensure that the database names are unique")
            }

            database.users.forEach { user ->
                if (database.users.count { user.name == it.name } > 1) {
                    return Error("duplicated user with name '${user.name}' found for database '${database.name}', ensure that the user names are unique")
                }
            }
        }

        return Success(
            PostgresSqlServiceConfigurationRuntime(
                configuration.name,
                configuration.databases.map {
                    PostgresSqlServiceDatabaseConfigurationRuntime(it.name, it.users.map {
                        PostgresSqlServiceDatabaseUsersConfigurationRuntime(it.name)
                    })
                },
            ),
        )
    }

    override val supportedConfiguration = PostgresSqlServiceConfiguration::class

    override val supportedRuntime = PostgresSqlServiceConfigurationRuntime::class
}
