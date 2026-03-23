package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.ConfigurationFactory
import kotlin.reflect.KClass

interface ServiceRegistration<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {
    val supportedConfiguration: KClass<C>
    val supportedRuntime: KClass<R>

    fun createManager(): ServiceConfigurationManager<C, R>

    fun createConfigurationFactory(): ConfigurationFactory<C>

    val type: String
}

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>.managerForService(configuration: C): ServiceConfigurationManager<C, R> =
    this.singleOrNull { it.supportedConfiguration == configuration::class }
        ?.createManager() as ServiceConfigurationManager<C, R>?
        ?: throw RuntimeException("no manager found for '${configuration::class.qualifiedName}'")

fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>.managerForService(runtime: R): ServiceConfigurationManager<C, R> =
    this.singleOrNull { it.supportedRuntime == runtime::class }?.createManager()
            as ServiceConfigurationManager<C, R>?
        ?: throw RuntimeException("no manager found for '${runtime::class.qualifiedName}'")
