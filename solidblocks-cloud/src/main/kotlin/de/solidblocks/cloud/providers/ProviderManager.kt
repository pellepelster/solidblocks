package de.solidblocks.cloud.providers

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import java.nio.file.Path
import kotlin.reflect.KClass

data class CloudConfigurationContext(val environment: EnvironmentContext, val configFileDirectory: Path)

interface ProviderManager<C : ProviderConfiguration, R : ProviderConfigurationRuntime> {
    fun validateConfiguration(configuration: C, context: CloudConfigurationContext, log: LogContext): Result<R>

    fun createProvisioners(runtime: R): List<InfrastructureResourceProvisioner<*, *>>

    fun createLookupProviders(runtime: R): List<ResourceLookupProvider<*, *>> = emptyList()

    val supportedConfiguration: KClass<C>
}
