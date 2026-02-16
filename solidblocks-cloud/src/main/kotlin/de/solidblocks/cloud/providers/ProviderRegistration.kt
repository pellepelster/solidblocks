package de.solidblocks.cloud.providers

import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderConfiguration
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderConfigurationManager
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderRuntime
import kotlin.reflect.KClass

interface ProviderRegistration<
    C : ProviderConfiguration,
    R : ProviderRuntime,
    M : ProviderConfigurationManager<C, R>,
> {
  val supportedConfiguration: KClass<C>
  val supportedRuntime: KClass<R>

  fun createConfigurationManager(): M

  fun createConfigurationFactory(): ConfigurationFactory<C>

  val type: String
}

fun <C : ProviderConfiguration, R : ProviderRuntime> List<ProviderRegistration<*, *, *>>
    .managerForConfiguration(
    configuration: C,
): ProviderConfigurationManager<C, R> =
    this.singleOrNull { it.supportedConfiguration == configuration::class }
        ?.createConfigurationManager() as ProviderConfigurationManager<C, R>?
        ?: throw RuntimeException("no manager found for '${configuration::class.qualifiedName}'")

fun <C : ProviderConfiguration, R : ProviderRuntime> List<ProviderRegistration<*, *, *>>
    .managerForRuntime(
    runtime: R,
): ProviderConfigurationManager<C, R> =
    this.singleOrNull { it.supportedRuntime == runtime::class }?.createConfigurationManager()
        as ProviderConfigurationManager<C, R>?
        ?: throw RuntimeException("no manager found for '${runtime::class.qualifiedName}'")

fun List<ProviderRegistration<*, *, *>>.managerForConfiguration(
    configuration: SSHKeyProviderConfiguration,
): SSHKeyProviderConfigurationManager<SSHKeyProviderConfiguration, SSHKeyProviderRuntime> =
    this.singleOrNull { it.supportedConfiguration == configuration::class }
        ?.createConfigurationManager()
        as SSHKeyProviderConfigurationManager<SSHKeyProviderConfiguration, SSHKeyProviderRuntime>?
        ?: throw RuntimeException("no manager found for '${configuration::class.qualifiedName}'")
