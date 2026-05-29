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
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.types.backup.createBackupConfiguration
import de.solidblocks.cloud.providers.types.backup.createBackupResources
import de.solidblocks.cloud.provisioner.context.*
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.postgres.database.PostgresDatabase
import de.solidblocks.cloud.provisioner.postgres.user.PostgresUser
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.RandomSecret
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.toResult
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfiguration
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseConfigurationRuntime
import de.solidblocks.cloud.services.postgres.model.PostgresSqlServiceDatabaseUsersConfigurationRuntime
import de.solidblocks.cloud.status.ServerStatus
import de.solidblocks.cloud.status.serverStatusMarkdown
import de.solidblocks.cloud.status.withServerStatus
import de.solidblocks.cloud.utils.*
import de.solidblocks.cloudinit.PostgresqlUserData
import de.solidblocks.cloudinit.PostgresqlUserData.Companion.BACKUP_STATUS_COMMAND
import de.solidblocks.shell.pgbackrest.PgBackRestInfo
import de.solidblocks.shell.pgbackrest.parsePgBackRestInfoOutput
import de.solidblocks.ssh.ensureCommand
import de.solidblocks.utils.LogContext
import java.nio.file.Path
import java.time.Duration

class PostgresSqlServiceManager : ServiceManager<PostgresSqlServiceConfiguration, PostgresSqlServiceConfigurationRuntime> {

    override fun maintenance(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: SSHProvisionerContext, log: LogContext): Result<Unit> =
        serverMaintenance(cloud, runtime, context, log)

    fun sanitizeEnvironmentVariables(input: String) = input.replace(Regex("[^a-zA-Z0-9]"), "_").uppercase()

    private fun superUserPasswordSecret(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime): GenericSecret<GenericSecretRuntime> = GenericSecret<GenericSecretRuntime>(
        secretPath(cloud.environmentContext, runtime, listOf("superuser", "password")),
        RandomSecret(),
        true,
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> {
        val serverName = serverName(cloud.environmentContext, runtime.name, 0)

        val result = context.withServerStatus(serverName) { sshClient, status ->
            status to sshClient.ensureCommand(BACKUP_STATUS_COMMAND).parsePgBackRestInfoOutput()
        }

        return markdown {
            h1("Service ${runtime.name}")
            h2("Server $serverName")
            serverStatusMarkdown(
                result.map {
                    it.first
                },
            )

            h2("Backups")

            when (result) {
                is Error<Pair<ServerStatus, PgBackRestInfo>> -> {
                    text("*failed to retrieve backup status*: **${result.error}**")
                }

                is Success<Pair<ServerStatus, PgBackRestInfo>> -> {
                    table {
                        header("start", "duration", "size")
                        result.data.second.flatMap {
                            it.backup
                        }.forEach {
                            row(it.timestamp.start.formatLocale(), Duration.between(it.timestamp.start, it.timestamp.stop).formatLocale(), it.info.size.formatBytes())
                        }
                    }
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
                    it.ensureLookup(HetznerServerLookup(serverName(cloud.environmentContext, runtime.name, 0)))
                        .privateIpv4 ?: throw RuntimeException("no private ip address found")
                },
            ),
            EnvironmentVariableStatic(
                sanitizeEnvironmentVariables("DATABASE_PORT"),
                "database port for service '${runtime.name}'",
                "5432",
            ),
        )

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: SSHProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            listOf(ServerInfo(sshConnectCommand(context, cloud, runtime, 0))),
            runtime.databases.map { EndpointInfo("jdbc", jdbcEndpoint(cloud, runtime, context, it)) },
        ),
    )

    fun jdbcEndpoint(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: ProvisionerContext, database: PostgresSqlServiceDatabaseConfigurationRuntime) =
        "jdbc:postgresql://${context.lookup(HetznerServerLookup(serverName(cloud.environmentContext, runtime.name, 0)))?.privateIpv4 ?: "<unknown>"}/${database.name}"

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> = Success(
        markdown {
            val sshConfigFilePath = Path.of(".").toAbsolutePath().relativize(sshConfigFilePath(cloud.context.configFileDirectory, context.environment))
            val serverName = serverName(cloud.environmentContext, runtime.name, 0)

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

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, context: ProvisionerContext): Result<List<BaseInfrastructureResource<*>>> {
        val serverName = serverName(cloud.environmentContext, runtime.name, 0)

        val defaultResources = createDefaultServerResources(cloud, runtime, 0)
        val backupResources = createBackupResources(cloud.backupProviderRuntime(), cloud, serverName, runtime, context.environment)

        val superUserPassword = superUserPasswordSecret(cloud, runtime)

        val configurationEnvironmentVars = (cloud.environmentVars + runtime.environmentVars).map {
            val resolvedValue = when (val result = context.interpolationRegistry().resolve(it.value)) {
                is Error<String> -> return Error(result.error)
                is Success<String> -> result.data
            }

            it.key to resolvedValue
        }.toMap()

        val userData =
            UserData(
                setOf(defaultResources.volumes.data, superUserPassword) + backupResources.first,
                {
                    PostgresqlUserData(
                        runtime.name,
                        configurationEnvironmentVars,
                        it.ensureLookup(superUserPassword.asLookup()).secret,
                        it.ensureLookup(defaultResources.volumes.data.asLookup()).device,
                        createBackupConfiguration(cloud.backupProviderRuntime(), cloud, runtime, context, backupResources.second),
                        runtime.majorVersion,
                        solidblocksVersion(),
                        context.ensureOptionalLookup(defaultResources.floatingIp?.asLookup())?.ip,
                    ).toResult(it, defaultResources.sshIdentity)
                },
            )

        val server =
            HetznerServer(
                serverName,
                userData = userData,
                location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
                sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud.environmentContext))),
                volumes = setOf(defaultResources.volumes.data.asLookup()) + setOfNotNull(backupResources.second?.asLookup()),
                type = cloud.hetznerProviderRuntime().defaultInstanceType,
                subnet =
                HetznerSubnetLookup(
                    defaultServiceSubnet,
                    HetznerNetworkLookup(networkName(cloud.environmentContext)),
                ),
                privateIp = serverPrivateIp(runtime.index),
                labels = serviceLabels(runtime) + cloudLabels(cloud.environmentContext),
                dependsOn = backupResources.first + defaultResources.list(),
                floatingIp = defaultResources.floatingIp?.asLookup(),
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

        return Success(listOf(server) + databaseResources + backupResources.first + defaultResources.list())
    }

    fun defaultDatabaseUserPassword(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, database: PostgresSqlServiceDatabaseConfigurationRuntime) =
        GenericSecret<GenericSecretRuntime>(secretPath(cloud.environmentContext, runtime, listOf(database.name, "password")), RandomSecret(), true)

    fun defaultDatabaseUserName(cloud: CloudConfigurationRuntime, runtime: PostgresSqlServiceConfigurationRuntime, database: PostgresSqlServiceDatabaseConfigurationRuntime) = database.name

    override fun createProvisioners(runtime: PostgresSqlServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *, *>>()

    override fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: PostgresSqlServiceConfiguration,
        context: ValidationContext,
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
                configuration.common.toRuntime(),
                BackupRuntime.fromConfig(configuration.backup),
                configuration.databases.map {
                    PostgresSqlServiceDatabaseConfigurationRuntime(
                        it.name,
                        it.users.map { PostgresSqlServiceDatabaseUsersConfigurationRuntime(it.name) },
                    )
                },
                configuration.majorVersion,
            ),
        )
    }

    override val supportedConfiguration = PostgresSqlServiceConfiguration::class

    override val supportedRuntime = PostgresSqlServiceConfigurationRuntime::class
}
