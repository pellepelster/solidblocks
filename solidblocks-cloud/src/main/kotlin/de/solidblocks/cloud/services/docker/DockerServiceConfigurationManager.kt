package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext

class DockerServiceConfigurationManager() :
    ServiceConfigurationManager<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {

    override fun createResources(
        cloud: CloudConfigurationRuntime,
        runtime: DockerServiceConfigurationRuntime
    ): List<BaseInfrastructureResource<*>> {
        return emptyList()
    }

    override fun createProvisioners(runtime: DockerServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>()

    override fun validatConfiguration(index: Int, cloud: CloudConfiguration, service: DockerServiceConfiguration, context: ProvisionerContext, log: LogContext): Result<DockerServiceConfigurationRuntime> {

        service.endpoints.forEach { endpoint ->
            if (service.endpoints.count { endpoint.port == it.port } > 1) {
                return Error("duplicated port config for port '${endpoint.port}'")
            }
        }

        return Success(
            DockerServiceConfigurationRuntime(
                index, service.name,
                service.endpoints.map {
                    DockerServiceEndpointConfigurationRuntime(it.port)
                },
            ),
        )
    }

    override val supportedConfiguration = DockerServiceConfiguration::class

    override val supportedRuntime = DockerServiceConfigurationRuntime::class
}
