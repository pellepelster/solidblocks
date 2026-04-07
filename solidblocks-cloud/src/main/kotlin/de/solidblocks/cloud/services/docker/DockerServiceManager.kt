package de.solidblocks.cloud.services.docker

import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.Constants.networkName
import de.solidblocks.cloud.Constants.serverIp
import de.solidblocks.cloud.Constants.serverName
import de.solidblocks.cloud.Constants.sshKeyName
import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolume
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.docker.model.DockerServiceConfiguration
import de.solidblocks.cloud.services.docker.model.DockerServiceConfigurationRuntime
import de.solidblocks.cloud.services.docker.model.DockerServiceEndpointConfigurationRuntime
import de.solidblocks.cloud.utils.ByteSize
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.docker.GenericDockerServiceUserData
import de.solidblocks.utils.LogContext

class DockerServiceManager :
    ServiceManager<DockerServiceConfiguration, DockerServiceConfigurationRuntime> {

  override fun createResources(
      cloud: CloudConfigurationRuntime,
      runtime: DockerServiceConfigurationRuntime,
  ): List<BaseInfrastructureResource<*>> {
    val dataVolume =
        HetznerVolume(
            serverName(cloud, runtime.name) + "-data",
            cloud.hetznerProviderConfig().defaultLocation,
            ByteSize.fromGigabytes(runtime.dataVolumeSize),
            emptyMap(),
        )

    val userData =
        UserData(
            setOf(dataVolume),
            { context ->
              GenericDockerServiceUserData(
                      runtime.name,
                      context.ensureLookup(dataVolume.asLookup()).device,
                      cloud.rootDomain,
                      runtime.image,
                      runtime.endpoints.map { it.port to it.port }.toMap(),
                  )
                  .render()
            },
        )

    val server =
        HetznerServer(
            serverName(cloud, runtime.name),
            userData = userData,
            location = cloud.hetznerProviderConfig().defaultLocation,
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud))),
            volumes = setOf(dataVolume.asLookup()),
            type = cloud.hetznerProviderConfig().defaultInstanceType,
            subnet =
                HetznerSubnetLookup(
                    DEFAULT_SERVICE_SUBNET,
                    HetznerNetworkLookup(networkName(cloud)),
                ),
            privateIp = serverIp(runtime.index),
        )

    return listOf(server, dataVolume)
  }

  override fun createProvisioners(runtime: DockerServiceConfigurationRuntime) =
      listOf<InfrastructureResourceProvisioner<*, *>>()

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
            configuration.dataVolumeSize,
            configuration.endpoints.map { DockerServiceEndpointConfigurationRuntime(it.port) },
            configuration.links,
        ),
    )
  }

  override val supportedConfiguration = DockerServiceConfiguration::class

  override val supportedRuntime = DockerServiceConfigurationRuntime::class
}
