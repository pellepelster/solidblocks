package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.ConfigurationFactory
import kotlin.reflect.KClass

interface ServiceRegistration<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {
  val supportedConfiguration: KClass<C>
  val supportedRuntime: KClass<R>

  fun createManager(): ServiceManager<C, R>

  fun createConfigurationFactory(): ConfigurationFactory<C>

  val type: String
}

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>
    .managerForService(configuration: C): ServiceManager<C, R> =
    this.singleOrNull { it.supportedConfiguration == configuration::class }?.createManager()
        as ServiceManager<C, R>?
        ?: throw RuntimeException("no manager found for '${configuration::class.qualifiedName}'")

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>
    .managerForService(runtime: R): ServiceManager<C, R> =
    this.singleOrNull { it.supportedRuntime == runtime::class }?.createManager()
        as ServiceManager<C, R>?
        ?: throw RuntimeException("no manager found for '${runtime::class.qualifiedName}'")
