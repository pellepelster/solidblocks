package de.solidblocks.cloud.services

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface ServiceManager<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {

  fun linkedEnvironmentVariables(
      cloud: CloudConfigurationRuntime,
      runtime: R,
  ): List<EnvironmentVariable> = emptyList()

  fun createResources(
      cloud: CloudConfigurationRuntime,
      runtime: R,
  ): List<BaseInfrastructureResource<*>>

  fun createProvisioners(runtime: R): List<InfrastructureResourceProvisioner<*, *>>

  fun validateConfiguration(
      index: Int,
      cloud: CloudConfiguration,
      configuration: C,
      context: CloudProvisionerContext,
      log: LogContext,
  ): Result<R>

  fun info(
      cloud: CloudConfigurationRuntime,
      runtime: R,
      context: CloudProvisionerContext,
  ): Result<String?> = Success(null)

  val supportedConfiguration: KClass<C>

  val supportedRuntime: KClass<R>
}

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>
    .forService(service: C): ServiceManager<C, R> =
    this.single { it.supportedConfiguration == service::class }.createManager()
        as ServiceManager<C, R>?
        ?: throw RuntimeException("no service found for '${service::class.qualifiedName}'")
