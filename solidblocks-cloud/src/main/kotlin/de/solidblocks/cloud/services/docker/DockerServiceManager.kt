package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.defaultServiceSubnet
import de.solidblocks.cloud.Constants.dnsRecordLabels
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.serverPrivateIp
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.types.backup.createBackupConfiguration
import de.solidblocks.cloud.providers.types.backup.createBackupResources
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.provisioner.context.ValidationContext
import de.solidblocks.cloud.provisioner.context.ensureLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.toResult
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfigurationRuntime
import de.solidblocks.cloud.status.ServerStatus
import de.solidblocks.cloud.status.serverStatusMarkdown
import de.solidblocks.cloud.status.withServerStatus
import de.solidblocks.cloud.utils.*
import de.solidblocks.cloudinit.GenericDockerServiceUserData
import de.solidblocks.cloudinit.RESTIC_STATUS_COMMAND
import de.solidblocks.shell.restic.ResticSnapshots
import de.solidblocks.shell.restic.parseResticSnapshotsOutput
import de.solidblocks.ssh.ensureCommand
import de.solidblocks.utils.LogContext
import java.time.Duration

class DockerServiceManager : ServiceManager<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {

    override fun maintenance(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: SSHProvisionerContext, log: LogContext): Result<Unit> =
        serverMaintenance(cloud, runtime, context, log)

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: SSHProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            listOf(ServerInfo(sshConnectCommand(context, cloud, runtime, 0))),
            listOf(EndpointInfo("web", endpoint(cloud, runtime, context))),
        ),
    )

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> = Success(
        markdown {
            h1("Service '${runtime.name}'")

            h2("Servers")
            text("to access server **${serverName(cloud.environmentContext, runtime.name, 0)}** via SSH, run")
            codeBlock(sshConnectCommand(context, cloud, runtime, 0))

            h2("Endpoints")
            list { item(endpoint(cloud, runtime, context)) }
        },
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: SSHProvisionerContext): Result<String> {
        val serverName = serverName(cloud.environmentContext, runtime.name, 0)

        val result = context.withServerStatus(serverName) { sshClient, status ->
            status to sshClient.ensureCommand(RESTIC_STATUS_COMMAND).parseResticSnapshotsOutput()
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
                is Error<Pair<ServerStatus, ResticSnapshots>> -> {
                    text("**failed to retrieve backup status**")
                }

                is Success<Pair<ServerStatus, ResticSnapshots>> -> {
                    table {
                        header("start", "duration", "size")
                        result.data.second.forEach {
                            row(it.summary.backupStart.formatLocale(), Duration.between(it.summary.backupStart, it.summary.backupEnd).formatLocale(), it.summary.totalBytesProcessed.formatBytes())
                        }
                    }
                }
            }
        }.let { Success(it) }
    }

    fun endpoint(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: ProvisionerContext) = if (cloud.dnsEnabled == true) {
        "https://${serverName(cloud.environmentContext, runtime.name, 0)}.${cloud.rootDomain}"
    } else {
        "http://${context.lookup(HetznerServerLookup(serverName(cloud.environmentContext, runtime.name, 0)))?.publicIpv4 ?: "<unknown>"}"
    }

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: ProvisionerContext): List<BaseInfrastructureResource<*>> {
        val environmentVariables = runtime.links.flatMap { link ->
            val links = cloud.services.filter { it.name == link }
            links.flatMap {
                context.managerForService<ServiceConfiguration, ServiceConfigurationRuntime>(it).linkedEnvironmentVariables(cloud, it)
            }
        }

        val serverName = serverName(cloud.environmentContext, runtime.name, 0)

        val defaultResources = this.createDefaultServerResources(cloud, runtime, 0)
        val backupResources = createBackupResources(cloud.backupProviderRuntime(), cloud, serverName, runtime, context.environment)

        val userData = UserData(
            setOf(defaultResources.volumes.data) + backupResources.first,
            { context ->
                GenericDockerServiceUserData(
                    runtime.name,
                    cloud.environmentVars + runtime.environmentVars + environmentVariables.associate {
                        val value = when (it) {
                            is EnvironmentVariableCallback -> it.value.invoke(context)
                            is EnvironmentVariableStatic -> it.value
                        }

                        it.name to value
                    },
                    context.ensureLookup(defaultResources.volumes.data.asLookup()).device,
                    createBackupConfiguration(cloud.backupProviderRuntime(), cloud, runtime, context, backupResources.second),
                    runtime.image,
                    runtime.endpoints.associate { 80 to it.port },
                    serverFQDN = cloud.rootDomain?.let { "${serverName(cloud.environmentContext, runtime.name, 0)}.$it" },
                ).toResult(context, defaultResources.sshIdentity)
            },
        )

        val server = HetznerServer(
            serverName,
            userData = userData,
            location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud.environmentContext))),
            volumes = setOf(defaultResources.volumes.data.asLookup()) + setOfNotNull(backupResources.second?.asLookup()),
            type = cloud.hetznerProviderRuntime().defaultInstanceType,
            subnet = HetznerSubnetLookup(
                defaultServiceSubnet,
                HetznerNetworkLookup(networkName(cloud.environmentContext)),
            ),
            privateIp = serverPrivateIp(runtime.index),
            labels = serviceLabels(runtime) + cloudLabels(cloud.environmentContext),
            dependsOn = backupResources.first + defaultResources.list(),
        )

        val optionalResources = if (cloud.rootDomain != null) {
            val zone = HetznerDnsZoneLookup(cloud.rootDomain)
            val serverDnsRecord = HetznerDnsRecord(
                serverName(cloud.environmentContext, runtime.name, 0),
                zone,
                listOf(server.asLookup()),
                labels = dnsRecordLabels(runtime) + cloudLabels(cloud.environmentContext),
            )
            listOf(serverDnsRecord, runtime.firewall(cloud, listOf(80, 443)))
        } else {
            listOf(runtime.firewall(cloud, listOf(80)))
        }

        return listOf(server) + optionalResources + setOfNotNull(backupResources.second) + defaultResources.list()
    }

    override fun createProvisioners(runtime: DockerServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *, *>>()

    override fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: DockerServiceConfiguration,
        context: ValidationContext,
        log: LogContext,
    ): Result<DockerServiceConfigurationRuntime> {
        configuration.links.forEach { link ->
            if (cloud.services.none { it.name == link }) {
                return Error("linked service '$link' not found for service '${configuration.name}'")
            }
        }

        configuration.links.forEach { link ->
            if (configuration.name == link) {
                return Error("service can not be linked with itself '$link' -> '$link'")
            }
        }

        if (configuration.endpoints.size > 1) {
            return Error("more than one endpoint is currently not supported for service '${configuration.name}'")
        }

        if (configuration.endpoints.size < 1) {
            return Error("no endpoint configured for service '${configuration.name}'")
        }

        configuration.endpoints.forEach { endpoint ->
            if (configuration.endpoints.count { endpoint.port == it.port } > 1) {
                return Error(
                    "duplicated port config for port '${endpoint.port}' for service '${configuration.name}'",
                )
            }
        }

        return Success(
            DockerServiceConfigurationRuntime(
                index,
                configuration.name,
                configuration.environmentVars,
                configuration.image,
                InstanceRuntime.fromConfig(configuration.instance),
                BackupRuntime.fromConfig(configuration.backup),
                configuration.endpoints.map { DockerServiceEndpointConfigurationRuntime(it.port) },
                configuration.links,
            ),
        )
    }

    override val supportedConfiguration = DockerServiceConfiguration::class

    override val supportedRuntime = DockerServiceConfigurationRuntime::class
}
