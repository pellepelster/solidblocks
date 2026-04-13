package de.solidblocks.cloud.services

import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshConfigFilePath
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable data class EndpointInfo(val type: String, val url: String)

@Serializable data class ServerInfo(val sshConnectCommand: String)

@Serializable
data class ServiceInfo(val name: String, val servers: List<ServerInfo>, val endpoints: List<EndpointInfo>)

@Serializable data class CloudInfo(val services: List<ServiceInfo>)

fun sshConnectCommand(context: CloudProvisionerContext, cloud: CloudConfigurationRuntime, runtime: ServiceConfigurationRuntime): String =
    "ssh -F ${sshConfigFilePath(context.sshConfigFilePath, context.environment.cloud).toAbsolutePath()} ${serverName(cloud, runtime.name)}"
