package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext

class DockerServiceManager() :
    ServiceManager<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {

    override fun createResources(
        cloud: CloudConfigurationRuntime,
        runtime: DockerServiceConfigurationRuntime
    ): List<BaseInfrastructureResource<*>> {
        return emptyList()
    }

    override fun createProvisioners(runtime: DockerServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>()

    override fun validatConfiguration(index: Int, cloud: CloudConfiguration, configuration: DockerServiceConfiguration, context: ProvisionerContext, log: LogContext): Result<DockerServiceConfigurationRuntime> {

        configuration.links.forEach { link ->
            if (cloud.services.none { it.name == link }) {
                return Error("linked service '${link}' not found for service '${configuration.name}'")
            }
        }

        configuration.links.forEach { link ->
            if (configuration.name == link) {
                return Error("service can not be linked with itself '${link}' -> '${link}'")
            }
        }

        configuration.endpoints.forEach { endpoint ->
            if (configuration.endpoints.count { endpoint.port == it.port } > 1) {
                return Error("duplicated port config for port '${endpoint.port}' for service '${configuration.name}'")
            }
        }

        return Success(
            DockerServiceConfigurationRuntime(
                index, configuration.name,
                configuration.endpoints.map {
                    DockerServiceEndpointConfigurationRuntime(it.port)
                },
                configuration.links
            )
        )
    }

    override val supportedConfiguration = DockerServiceConfiguration::class

    override val supportedRuntime = DockerServiceConfigurationRuntime::class
}
