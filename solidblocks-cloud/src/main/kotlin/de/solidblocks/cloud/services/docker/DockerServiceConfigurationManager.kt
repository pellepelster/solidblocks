package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.garagefs.accesskey.GarageFsAccessKeyProvisioner
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketProvisioner
import de.solidblocks.cloud.provisioner.garagefs.permission.GarageFsPermissionProvisioner
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext

class DockerServiceConfigurationManager(val cloudConfiguration: CloudConfigurationRuntime) :
    ServiceConfigurationManager<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {

    override fun createResources(
        runtime: DockerServiceConfigurationRuntime
    ): List<BaseInfrastructureResource<*>> {
        return emptyList()
    }

    override fun createProvisioners(runtime: DockerServiceConfigurationRuntime) =
        listOf<InfrastructureResourceProvisioner<*, *>>(GarageFsBucketProvisioner(), GarageFsAccessKeyProvisioner(), GarageFsPermissionProvisioner())

    override fun validatConfiguration(
        configuration: DockerServiceConfiguration,
        context: LogContext,
    ): Result<DockerServiceConfigurationRuntime> {

        configuration.endpoints.forEach { endpoint ->
            if (configuration.endpoints.count { endpoint.port == it.port } > 1) {
                return Error("duplicated port config for port '${endpoint.port}'")
            }
        }

        return Success(
            DockerServiceConfigurationRuntime(
                configuration.name,
                configuration.endpoints.map {
                    DockerServiceEndpointConfigurationRuntime(it.port)
                },
            ),
        )
    }

    override val supportedConfiguration = DockerServiceConfiguration::class

    override val supportedRuntime = DockerServiceConfigurationRuntime::class
}
