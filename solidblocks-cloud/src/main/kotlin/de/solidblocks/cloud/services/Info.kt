package de.solidblocks.cloud.services

import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshConfigFilePath
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import java.nio.file.Path
import kotlinx.serialization.Serializable

@Serializable data class EndpointInfo(val url: String)

@Serializable data class ServerInfo(val sshConnectCommand: String)

@Serializable
data class ServiceInfo(
    val name: String,
    val servers: List<ServerInfo>,
    val endpoints: List<EndpointInfo>,
)

@Serializable data class CloudInfo(val services: List<ServiceInfo>)

fun sshConnectCommand(
    context: CloudProvisionerContext,
    cloud: CloudConfigurationRuntime,
    runtime: ServiceConfigurationRuntime,
): String =
    "ssh -F ${Path.of(".").toAbsolutePath().relativize(sshConfigFilePath(context.sshConfigFilePath, context.cloudName))} ${serverName(cloud, runtime.name)}"
