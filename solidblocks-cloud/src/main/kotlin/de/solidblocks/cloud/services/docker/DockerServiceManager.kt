package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.Constants.cloudLabels
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.serverIp
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.serviceLabels
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.markdown
import de.solidblocks.cloud.providers.types.backup.createBackupConfiguration
import de.solidblocks.cloud.providers.types.backup.createBackupResources
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.restic.model.parseResticSnapshotsOutput
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfigurationRuntime
import de.solidblocks.cloud.utils.*
import de.solidblocks.cloudinit.docker.GenericDockerServiceUserData
import de.solidblocks.restic.RESTIC_STATUS_COMMAND
import de.solidblocks.utils.LogContext
import java.time.Duration

class DockerServiceManager : ServiceManager<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {

    override fun infoJson(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: CloudProvisionerContext) = Success(
        ServiceInfo(
            runtime.name,
            listOf(ServerInfo(sshConnectCommand(context, cloud, runtime))),
            listOf(EndpointInfo(endpoint(cloud, runtime, context))),
        ),
    )

    override fun infoText(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: CloudProvisionerContext): Result<String> = Success(
        markdown {
            h1("Service '${runtime.name}'")

            h2("Servers")
            text("to access server **${serverName(cloud, runtime.name)}** via SSH, run")
            codeBlock(sshConnectCommand(context, cloud, runtime))

            h2("Endpoints")
            list { item(endpoint(cloud, runtime, context)) }
        },
    )

    override fun status(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: CloudProvisionerContext): Result<String> {
        val result = context.createOrGetSshClient(HetznerServerLookup(serverName(cloud, runtime.name))).command(RESTIC_STATUS_COMMAND)
        if (result.exitCode != 0) {
            return Error<String>("command failed '${result.stdErr}'")
        }

        val backupStatus = result.stdOut.parseResticSnapshotsOutput()

        return markdown {
            h1("Service '${runtime.name}'")
            h2("Backups")

            table {
                header("start", "duration", "size")
                backupStatus.forEach {
                    row(it.summary.backupStart.formatLocale(), Duration.between(it.summary.backupStart, it.summary.backupEnd).formatLocale(), it.summary.totalBytesProcessed.formatBytes())
                }
            }
        }.let { Success(it) }
    }

    fun endpoint(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: CloudProvisionerContext) = if (cloud.dnsEnabled == true) {
        "http://${serverName(cloud, runtime.name)}.${cloud.rootDomain}"
    } else {
        "http://${context.lookup(HetznerServerLookup(serverName(cloud, runtime.name)))?.publicIpv4 ?: "<unknown>"}"
    }

    override fun createResources(cloud: CloudConfigurationRuntime, runtime: DockerServiceConfigurationRuntime, context: CloudProvisionerContext): List<BaseInfrastructureResource<*>> {
        val environmentVariables = runtime.links.flatMap { link ->
            val links = cloud.services.filter { it.name == link }
            links.flatMap {
                context.managerForService<ServiceConfiguration, ServiceConfigurationRuntime>(it).linkedEnvironmentVariables(cloud, it)
            }
        }

        val serverName = serverName(cloud, runtime.name)
        val dataVolume = HetznerVolume(
            serverName + "-data",
            runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            ByteSize.fromGigabytes(runtime.instance.volumeSize),
            emptyMap(),
        )

        val backupResources = createBackupResources(cloud.backupProviderRuntime(), cloud, serverName, runtime, context.environment)

        val userData = UserData(
            setOf(dataVolume) + backupResources.first,
            { context ->
                GenericDockerServiceUserData(
                    runtime.name,
                    context.ensureLookup(dataVolume.asLookup()).device,
                    createBackupConfiguration(cloud.backupProviderRuntime(), cloud, runtime, context, backupResources.second),
                    runtime.image,
                    runtime.endpoints.associate { 80 to it.port },
                    serverFQDN = cloud.rootDomain?.let { "${serverName(cloud, runtime.name)}.$it" },
                    environmentVariables = environmentVariables.associate {
                        val value = when (it) {
                            is EnvironmentVariableCallback -> it.value.invoke(context)
                            is EnvironmentVariableStatic -> it.value
                        }

                        it.name to value
                    },
                ).render()
            },
        )

        val server = HetznerServer(
            serverName,
            userData = userData,
            location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud))),
            volumes = setOf(dataVolume.asLookup()) + setOfNotNull(backupResources.second?.asLookup()),
            type = cloud.hetznerProviderRuntime().defaultInstanceType,
            subnet = HetznerSubnetLookup(
                DEFAULT_SERVICE_SUBNET,
                HetznerNetworkLookup(networkName(cloud)),
            ),
            privateIp = serverIp(runtime.index),
            labels = serviceLabels(runtime) + cloudLabels(cloud),
            dependsOn = backupResources.first,
        )

        val optionalResources =
            if (cloud.rootDomain != null) {
                val zone = HetznerDnsZoneLookup(cloud.rootDomain)
                val serverDnsRecord = HetznerDnsRecord(
                    serverName(cloud, runtime.name),
                    zone,
                    listOf(server.asLookup()),
                )
                listOf(serverDnsRecord, runtime.firewall(cloud, listOf(80, 443)))
            } else {
                listOf(runtime.firewall(cloud, listOf(80)))
            }

        return listOf(server, dataVolume) + optionalResources + setOfNotNull(backupResources.second)
    }

    override fun createProvisioners(runtime: DockerServiceConfigurationRuntime) = listOf<InfrastructureResourceProvisioner<*, *>>()

    override fun validateConfiguration(
        index: Int,
        cloud: CloudConfiguration,
        configuration: DockerServiceConfiguration,
        context: CloudProvisionerContext,
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
