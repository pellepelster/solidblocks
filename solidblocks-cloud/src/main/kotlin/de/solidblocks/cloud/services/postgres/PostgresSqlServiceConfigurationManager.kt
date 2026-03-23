package de.solidblocks.cloud.services.postgres

import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.serverIp
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseUsersConfigurationRuntime
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.postgresql.PostgresqlUserData
import de.solidblocks.utils.LogContext

class PostgresSqlServiceConfigurationManager(val cloudConfiguration: CloudConfigurationRuntime) :
    ServiceConfigurationManager<PostgresSqlServiceConfiguration, PostgresSqlServiceConfigurationRuntime> {

    override fun createResources(
        runtime: PostgresSqlServiceConfigurationRuntime
    ): List<BaseInfrastructureResource<*>> {

        val dataVolume =
            HetznerVolume(serverName(cloudConfiguration, runtime.name) + "-data", cloudConfiguration.hetznerProviderConfig().defaultLocation, ByteSize.fromGigabytes(runtime.instanceSize), emptyMap())
        val backupVolume =
            HetznerVolume(serverName(cloudConfiguration, runtime.name) + "-backup", cloudConfiguration.hetznerProviderConfig().defaultLocation, ByteSize.fromGigabytes(runtime.backupRetention), emptyMap())


        val userData = UserData(
            setOf(dataVolume, backupVolume),
            { context ->
                PostgresqlUserData(
                    context.ensureLookup(dataVolume.asLookup()).device,
                    context.ensureLookup(backupVolume.asLookup()).device,
                    runtime.name
                ).render()
            },
        )

        val server = HetznerServer(
            serverName(cloudConfiguration, runtime.name),
            userData = userData,
            location = cloudConfiguration.hetznerProviderConfig().defaultLocation,
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloudConfiguration))),
            volumes = setOf(dataVolume.asLookup(), backupVolume.asLookup()),
            type = cloudConfiguration.hetznerProviderConfig().defaultInstanceType,
            subnet = HetznerSubnetLookup(DEFAULT_SERVICE_SUBNET, HetznerNetworkLookup(networkName(cloudConfiguration))),
            privateIp = serverIp(runtime.index)
        )

        return listOf(server)
    }

    override fun createProvisioners(runtime: PostgresSqlServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>()

    override fun validatConfiguration(index: Int, configuration: PostgresSqlServiceConfiguration, context: ProvisionerContext, log: LogContext): Result<PostgresSqlServiceConfigurationRuntime> {

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
                index,
                configuration.name,
                configuration.instanceSize,
                configuration.backupFullRetentionDays,
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
