package de.solidblocks.cloud.services

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface ServiceConfigurationManager<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {
  fun createResources(runtime: R): List<InfrastructureResource<*>>

  fun createProvisioners(runtime: R): List<InfrastructureResourceProvisioner<*, *>>

  fun validatConfiguration(configuration: C, context: LogContext): Result<R>

  val supportedConfiguration: KClass<C>
  val supportedRuntime: KClass<R>
}

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>
    .forService(
    service: C,
    cloudConfiguration: CloudConfigurationRuntime,
): ServiceConfigurationManager<C, R> =
    this.single { it.supportedConfiguration == service::class }.createManager(cloudConfiguration)
        as ServiceConfigurationManager<C, R>?
        ?: throw RuntimeException("no service found for '${service::class.qualifiedName}'")
