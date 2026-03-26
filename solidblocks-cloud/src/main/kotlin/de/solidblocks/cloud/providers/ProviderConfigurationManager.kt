package de.solidblocks.cloud.providers

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import java.nio.file.Path
import kotlin.reflect.KClass

data class CloudConfigurationContext(val configFileDirectory: Path)

interface ProviderConfigurationManager<C : ProviderConfiguration, R : ProviderConfigurtionRuntime> {

  fun validate(configuration: C, context: CloudConfigurationContext, log: LogContext): Result<R>

  fun createProvisioners(runtime: R): List<InfrastructureResourceProvisioner<*, *>>

  fun createLookupProviders(runtime: R): List<ResourceLookupProvider<*, *>> = emptyList()

  val supportedConfiguration: KClass<C>
}
