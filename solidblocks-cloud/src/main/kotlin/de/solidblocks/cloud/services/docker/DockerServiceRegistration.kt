package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationFactory
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime

class DockerServiceRegistration :
    ServiceRegistration<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {
    override val supportedConfiguration = DockerServiceConfiguration::class
    override val supportedRuntime = DockerServiceConfigurationRuntime::class

    override fun createManager(cloudConfiguration: CloudConfigurationRuntime) =
        DockerServiceConfigurationManager(cloudConfiguration)

    override fun createConfigurationFactory() = DockerServiceConfigurationFactory()

    override val type = "docker"
}
