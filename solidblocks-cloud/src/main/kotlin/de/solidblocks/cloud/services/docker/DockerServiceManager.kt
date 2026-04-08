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
import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.EnvironmentVariableCallback
import de.solidblocks.cloud.services.EnvironmentVariableStatic
import de.solidblocks.cloud.services.InstanceRuntime
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
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
      context: CloudProvisionerContext,
  ): List<BaseInfrastructureResource<*>> {
    val environmentVariables =
        runtime.links.flatMap { link ->
          val links = cloud.services.filter { it.name == link }
          links.flatMap {
            context
                .managerForService<ServiceConfiguration, ServiceConfigurationRuntime>(it)
                .linkedEnvironmentVariables(cloud, it)
          }
        }

    val dataVolume =
        HetznerVolume(
            serverName(cloud, runtime.name) + "-data",
            runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            ByteSize.fromGigabytes(runtime.instance.volumeSize),
            emptyMap(),
        )

    val backupVolume =
        HetznerVolume(
            serverName(cloud, runtime.name) + "-backup",
            runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            runtime.backup.backupVolumeSizeWithDefault(runtime.instance.volumeSize),
            emptyMap(),
        )

    val userData =
        UserData(
            setOf(dataVolume),
            { context ->
              GenericDockerServiceUserData(
                      runtime.name,
                      context.ensureLookup(dataVolume.asLookup()).device,
                      context.ensureLookup(backupVolume.asLookup()).device,
                      cloud.rootDomain,
                      runtime.image,
                      runtime.endpoints.associate { 80 to it.port },
                      environmentVariables =
                          environmentVariables.associate {
                            val value =
                                when (it) {
                                  is EnvironmentVariableCallback -> it.value.invoke(context)
                                  is EnvironmentVariableStatic -> it.value
                                }

                            it.name to value
                          },
                  )
                  .render()
            },
        )

    val server =
        HetznerServer(
            serverName(cloud, runtime.name),
            userData = userData,
            location = runtime.instance.locationWithDefault(cloud.hetznerProviderRuntime()),
            sshKeys = setOf(HetznerSSHKeyLookup(sshKeyName(cloud))),
            volumes = setOf(dataVolume.asLookup(), backupVolume.asLookup()),
            type = cloud.hetznerProviderRuntime().defaultInstanceType,
            subnet =
                HetznerSubnetLookup(
                    DEFAULT_SERVICE_SUBNET,
                    HetznerNetworkLookup(networkName(cloud)),
                ),
            privateIp = serverIp(runtime.index),
        )

    return listOf(server, dataVolume, backupVolume)
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
