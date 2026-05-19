package de.solidblocks.cloud.services.docker.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.ServiceCommonRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

data class DockerServiceEndpointConfigurationRuntime(val port: Int)

class DockerServiceConfigurationRuntime(
    override val index: Int,
    override val common: ServiceCommonRuntime,
    val image: String,
    override val backup: BackupRuntime,
    val endpoints: List<DockerServiceEndpointConfigurationRuntime>,
    val links: List<String>,
) : ServiceConfigurationRuntime
