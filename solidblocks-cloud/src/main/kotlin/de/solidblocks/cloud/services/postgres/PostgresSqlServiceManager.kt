package de.solidblocks.cloud.services.postgres

import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.serverIp
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.markdown
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabase
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUser
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.EnvironmentVariableCallback
import de.solidblocks.cloud.services.EnvironmentVariableStatic
import de.solidblocks.cloud.services.ServiceManager
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

class PostgresSqlServiceManager :
    ServiceManager<PostgresSqlServiceConfiguration, PostgresSqlServiceConfigurationRuntime> {

  fun sanitizeEnvironmentVariables(input: String) =
      input.replace(Regex("[^a-zA-Z0-9]"), "_").uppercase()

  override fun linkedEnvironmentVariables(
      cloud: CloudConfigurationRuntime,
      runtime: PostgresSqlServiceConfigurationRuntime,
  ) =
      runtime.databases.flatMap {
        val password = defaultDatabaseUserPassword(cloud, runtime, it).asLookup()

        listOf(
            EnvironmentVariableStatic(
                sanitizeEnvironmentVariables("${runtime.name}_${it.name}_USER"),
                "name of the default user for database '${it.name}'",
                defaultDatabaseUserName(cloud, runtime, it),
            ),
            EnvironmentVariableCallback(
                sanitizeEnvironmentVariables("${runtime.name}_${it.name}_PASSWORD"),
                "password for the default user of database '${it.name}'",
                { it.ensureLookup(password).secret },
            ),
        )
      } +
          listOf(
              EnvironmentVariableCallback(
                  sanitizeEnvironmentVariables("${runtime.name}_DATABASE_HOST"),
                  "host address for service '${runtime.name}'",
                  {
                    it.ensureLookup(HetznerServerLookup(serverName(cloud, runtime.name)))
                        .privateIpv4 ?: throw RuntimeException("no private ip address found")
                  },
              ),
              EnvironmentVariableStatic(
                  sanitizeEnvironmentVariables("${runtime.name}_DATABASE_PORT"),
                  "database port for service '${runtime.name}'",
                  "5432",
              ),
          )

  override fun info(
      cloud: CloudConfigurationRuntime,
      runtime: PostgresSqlServiceConfigurationRuntime,
      context: CloudProvisionerContext,
  ): Result<String?> =
      Success(
          markdown {
            h1("Service '${runtime.name}'")
            table {
              header("Name", "Description")
              linkedEnvironmentVariables(cloud, runtime).forEach { row(it.name, it.description) }
            }
          },
      )

  override fun createResources(
      cloud: CloudConfigurationRuntime,
      runtime: PostgresSqlServiceConfigurationRuntime,
  ): List<BaseInfrastructureResource<*>> {
    val dataVolume =
        HetznerVolume(
            serverName(cloud, runtime.name) + "-data",
            cloud.hetznerProviderConfig().defaultLocation,
            ByteSize.fromGigabytes(runtime.dataVolumeSize),
            emptyMap(),
        )
    val backupVolume =
        HetznerVolume(
            serverName(cloud, runtime.name) + "-backup",
            cloud.hetznerProviderConfig().defaultLocation,
            ByteSize.fromGigabytes(
                runtime.backupVolumeSize ?: (runtime.dataVolumeSize * runtime.backupRetention),
            ),
            emptyMap(),
        )

    val superUserPassword = PassSecret(secretPath(cloud, runtime, listOf("superuser", "password")))

    val userData =
        UserData(
            setOf(dataVolume, backupVolume, superUserPassword),
            { context ->
              PostgresqlUserData(
                      context.ensureLookup(dataVolume.asLookup()).device,
                      context.ensureLookup(backupVolume.asLookup()).device,
                      runtime.name,
                      context.ensureLookup(superUserPassword.asLookup()).secret,
                  )
                  .render()
            },
        )

    val server =
        HetznerServer(
            serverName(cloud, runtime.name),
            userData = userData,
            location = cloud.hetznerProviderConfig().defaultLocation,
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud))),
            volumes = setOf(dataVolume.asLookup(), backupVolume.asLookup()),
            type = cloud.hetznerProviderConfig().defaultInstanceType,
            subnet =
                HetznerSubnetLookup(
                    DEFAULT_SERVICE_SUBNET,
                    HetznerNetworkLookup(networkName(cloud)),
                ),
            privateIp = serverIp(runtime.index),
        )

    val databaseResources =
        runtime.databases.flatMap {
          val password = defaultDatabaseUserPassword(cloud, runtime, it)
          val user =
              PostgresUser(
                  defaultDatabaseUserName(cloud, runtime, it),
                  password.asLookup(),
                  server.asLookup(),
                  superUserPassword.asLookup(),
              )
          listOf(
              password,
              user,
              PostgresDatabase(
                  it.name,
                  user.asLookup(),
                  server.asLookup(),
                  superUserPassword.asLookup(),
              ),
          )
        }

    return listOf(server, dataVolume, backupVolume) + databaseResources
  }

  fun defaultDatabaseUserPassword(
      cloud: CloudConfigurationRuntime,
      runtime: PostgresSqlServiceConfigurationRuntime,
      database: PostgresSqlServiceDatabaseConfigurationRuntime,
  ) = PassSecret(secretPath(cloud, runtime, listOf(database.name, "password")))

  fun defaultDatabaseUserName(
      cloud: CloudConfigurationRuntime,
      runtime: PostgresSqlServiceConfigurationRuntime,
      database: PostgresSqlServiceDatabaseConfigurationRuntime,
  ) = database.name

  override fun createProvisioners(runtime: PostgresSqlServiceConfigurationRuntime) =
      listOf<InfrastructureResourceProvisioner<*, *>>()

  override fun validateConfiguration(
      index: Int,
      cloud: CloudConfiguration,
      service: PostgresSqlServiceConfiguration,
      context: CloudProvisionerContext,
      log: LogContext,
  ): Result<PostgresSqlServiceConfigurationRuntime> {
    service.databases.forEach { database ->
      if (service.databases.count { database.name == it.name } > 1) {
        return Error(
            "duplicated database with name '${database.name}', ensure that the database names are unique",
        )
      }

      database.users.forEach { user ->
        if (database.users.count { user.name == it.name } > 1) {
          return Error(
              "duplicated user with name '${user.name}' found for database '${database.name}', ensure that the user names are unique",
          )
        }
      }
    }

    return Success(
        PostgresSqlServiceConfigurationRuntime(
            index,
            service.name,
            service.dataVolumeSize,
            service.backupFullRetentionDays,
            service.backupVolumeSize,
            service.databases.map {
              PostgresSqlServiceDatabaseConfigurationRuntime(
                  it.name,
                  it.users.map { PostgresSqlServiceDatabaseUsersConfigurationRuntime(it.name) },
              )
            },
        ),
    )
  }

  override val supportedConfiguration = PostgresSqlServiceConfiguration::class

  override val supportedRuntime = PostgresSqlServiceConfigurationRuntime::class
}
