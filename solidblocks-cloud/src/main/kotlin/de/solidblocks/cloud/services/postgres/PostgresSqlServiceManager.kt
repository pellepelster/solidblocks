package de.solidblocks.cloud.services.postgres

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.defaultServiceSubnet
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.serverPrivateIp
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.Constants.sshConfigFilePath
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.Constants.volumeLabels
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.types.backup.createBackupConfiguration
import de.solidblocks.cloud.providers.types.backup.createBackupResources
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.RandomSecret
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabase
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUser
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseUsersConfigurationRuntime
import de.solidblocks.cloud.utils.*
import de.solidblocks.cloud.utils.markdown
import de.solidblocks.cloudinit.PostgresqlUserData
import de.solidblocks.cloudinit.PostgresqlUserData.Companion.BACKUP_STATUS_COMMAND
import de.solidblocks.shell.pgbackrest.parsePgBackRestInfoOutput
import de.solidblocks.shell.toCloudInit
import de.solidblocks.utils.LogContext
import java.nio.file.Path
import java.time.Duration
import kotlin.collections.plus

class PostgresSqlServiceManager : ServiceManager<PostgresSqlServiceConfiguration, PostgresSqlServiceConfigurationRuntime> {

    fun sanitizeEnvironmentVariables(input: String) = input.replace(Regex("[^a-zA-Z0-9]"), "_").uppercase()

    private fun superUserPasswordSecret(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime): PassSecret = PassSecret(
        secretPath(cloud, runtime, listOf("superuser", "password")),
        RandomSecret(),
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: CloudProvisionerContext): Result<String> {
        val result = context.createOrGetSshClient(HetznerServerLookup(serverName(cloud, runtime.name))).command(BACKUP_STATUS_COMMAND)
        if (result.exitCode != 0) {
            return Error<String>("command failed '${result.stdErr}'")
        }

        val backupStatus = result.stdOut.parsePgBackRestInfoOutput()

        return markdown {
            h1("Service '${runtime.name}'")
            h2("Backups")

            table {
                header("start", "duration", "size")
                backupStatus.flatMap {
                    it.backup
                }.forEach {
                    row(it.timestamp.start.formatLocale(), Duration.between(it.timestamp.start, it.timestamp.stop).formatLocale(), it.info.size.formatBytes())
                }
            }
        }.let { Success(it) }
    }

    override fun linkedEnvironmentVariables(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime) = runtime.databases.flatMap {
        val password = defaultDatabaseUserPassword(cloud, runtime, it).asLookup()

        listOf(
            EnvironmentVariableStatic(
                sanitizeEnvironmentVariables("DATABASE_USER"),
                "name of the default user for database '${it.name}'",
                defaultDatabaseUserName(cloud, runtime, it),
            ),
            EnvironmentVariableCallback(
                sanitizeEnvironmentVariables("DATABASE_PASSWORD"),
                "password for the default user of database '${it.name}'",
                { it.ensureLookup(password).secret },
            ),
        )
    } +
        listOf(
            EnvironmentVariableCallback(
                sanitizeEnvironmentVariables("DATABASE_HOST"),
                "host address for service '${runtime.name}'",
                {
                    it.ensureLookup(HetznerServerLookup(serverName(cloud, runtime.name)))
                        .privateIpv4 ?: throw RuntimeException("no private ip address found")
                },
            ),
            EnvironmentVariableStatic(
                sanitizeEnvironmentVariables("DATABASE_PORT"),
                "database port for service '${runtime.name}'",
                "5432",
            ),
        )

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: CloudProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            listOf(ServerInfo(sshConnectCommand(context, cloud, runtime))),
            runtime.databases.map { EndpointInfo("jdbc", jdbcEndpoint(cloud, runtime, context, it)) },
        ),
    )

    fun jdbcEndpoint(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: CloudProvisionerContext, database: PostgresSqlServiceDatabaseConfigurationRuntime) =
        "jdbc:postgresql://${context.lookup(HetznerServerLookup(serverName(cloud, runtime.name)))?.privateIpv4 ?: "<unknown>"}/${database.name}"

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: CloudProvisionerContext): Result<String> = Success(
        markdown {
            val sshConfigFilePath = Path.of(".").toAbsolutePath().relativize(sshConfigFilePath(context.sshConfigFilePath, context.environment.cloud))
            val serverName = serverName(cloud, runtime.name)

            h1("Service '${runtime.name}'")

            h2("Servers")
            text("to access server **$serverName** via SSH, run")
            codeBlock(
                "ssh -F ${sshConfigFilePath.toAbsolutePath()} $serverName",
            )

            h2("Endpoints")
            list {
                runtime.databases.forEach { item("JDBC: ${jdbcEndpoint(cloud, runtime, context, it)}") }
            }

            h2("Database")

            text("to open a tunnel to the database run")
            codeBlock(
                """
                ssh -F ${sshConfigFilePath.toAbsolutePath()} -L 5432:localhost:5432 $serverName
                """.trimIndent(),
            )
            text("\n")

            text("connect to the database **postgres**")
            codeBlock(
                """
                ${superUserPasswordSecret(cloud, runtime).shellExportCommand("PGPASSWORD")}
                psql --host localhost --user rds postgres
                """.trimIndent(),
            )

            text("\n")

            runtime.databases.forEach {
                text("connect to the database **${it.name}** run")
                codeBlock(
                    """
                ${defaultDatabaseUserPassword(cloud, runtime, it).shellExportCommand("PGPASSWORD")}
                psql --host localhost --user ${defaultDatabaseUserName(cloud, runtime, it)} ${it.name}
                    """.trimIndent(),
                )
            }

            /*
            table {
                header("Name", "Description")
                linkedEnvironmentVariables(cloud, runtime).forEach { row(it.name, it.description) }
            }*/
        },
    )

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: CloudProvisionerContext): List<BaseInfrastructureResource<*>> {
        val serverName = serverName(cloud, runtime.name)

        val defaultResources = createDefaultResources(cloud, runtime)
        val backupResources = createBackupResources(cloud.backupProviderRuntime(), cloud, serverName, runtime, context.environment)

        val superUserPassword = superUserPasswordSecret(cloud, runtime)

        val userData =
            UserData(
                setOf(defaultResources.dataVolume, superUserPassword) + backupResources.first,
                { context ->
                    PostgresqlUserData(
                        runtime.name,
                        context.ensureLookup(superUserPassword.asLookup()).secret,
                        context.ensureLookup(defaultResources.dataVolume.asLookup()).device,
                        createBackupConfiguration(cloud.backupProviderRuntime(), cloud, runtime, context, backupResources.second),
                    ).shellScript().toCloudInit(
                        context.ensureLookup(defaultResources.sshIdentityRsaSecret.asLookup()).secret,
                        context.ensureLookup(defaultResources.sshIdentityED25519Secret.asLookup()).secret,
                    ).render()
                },
            )

        val server =
            HetznerServer(
                serverName,
                userData = userData,
                location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
                sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud))),
                volumes = setOf(defaultResources.dataVolume.asLookup()) + setOfNotNull(backupResources.second?.asLookup()),
                type = cloud.hetznerProviderRuntime().defaultInstanceType,
                subnet =
                HetznerSubnetLookup(
                    defaultServiceSubnet,
                    HetznerNetworkLookup(networkName(cloud)),
                ),
                privateIp = serverPrivateIp(runtime.index),
                labels = serviceLabels(runtime) + cloudLabels(cloud),
                dependsOn = backupResources.first,
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

        return listOf(server) + databaseResources + backupResources.first + defaultResources.list()
    }

    fun defaultDatabaseUserPassword(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, database: PostgresSqlServiceDatabaseConfigurationRuntime) =
        PassSecret(secretPath(cloud, runtime, listOf(database.name, "password")), RandomSecret())

    fun defaultDatabaseUserName(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, database: PostgresSqlServiceDatabaseConfigurationRuntime) = database.name

    override fun createProvisioners(runtime: PostgresSqlServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *>>()

    override fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: PostgresSqlServiceConfiguration,
        context: CloudProvisionerContext,
        log: LogContext,
    ): Result<PostgresSqlServiceConfigurationRuntime> {
        configuration.databases.forEach { database ->
            if (configuration.databases.count { database.name == it.name } > 1) {
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
                configuration.name,
                InstanceRuntime.fromConfig(configuration.instance),
                BackupRuntime.fromConfig(configuration.backup),
                configuration.databases.map {
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
