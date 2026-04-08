package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.InstanceRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class DockerServiceEndpointConfigurationRuntime(val port: Int)

data class DockerServiceConfigurationRuntime(
    override val index: Int,
    override val name: String,
    val image: String,
    val instance: InstanceRuntime,
    val backup: BackupRuntime,
    val endpoints: List<DockerServiceEndpointConfigurationRuntime>,
    val links: List<String>,
) : ServiceConfigurationRuntime
