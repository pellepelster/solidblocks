package de.solidblocks.cloud.providers

import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.ResourceProvisioner
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.log.LogContext
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import java.nio.file.Path
import kotlin.reflect.KClass

data class CloudConfigurationContext(val environment: EnvironmentContext, val configFileDirectory: Path)

interface ProviderManager<C : ProviderConfiguration, R : ProviderConfigurationRuntime> {
    fun validateConfiguration(configuration: C, context: CloudConfigurationContext, log: LogContext): Result<R>

    fun createProvisioners(runtime: R): List<ResourceProvisioner<*, *, *>>

    fun createLookupProviders(runtime: R): List<ResourceLookupProvider<*, *>> = emptyList()

    val supportedConfiguration: KClass<C>
}
