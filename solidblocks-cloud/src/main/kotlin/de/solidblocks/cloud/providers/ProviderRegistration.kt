package de.solidblocks.cloud.providers

import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderConfiguration
import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderConfigurationRuntime
import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderManager
import kotlin.reflect.KClass

interface ProviderRegistration<C : ProviderConfiguration, R : ProviderConfigurationRuntime, M : ProviderManager<C, R>> {

    val type: String

    val supportedConfiguration: KClass<C>
    val supportedRuntime: KClass<R>

    fun createManager(): M
    fun createFactory(): ConfigurationFactory<C>
}

@Suppress("UNCHECKED_CAST")
fun <C : ProviderConfiguration, R : ProviderConfigurationRuntime> List<
    ProviderRegistration<*, *, *>,
    >.managerForConfiguration(configuration: C): ProviderManager<C, R> =
    this.singleOrNull { it.supportedConfiguration == configuration::class }
        ?.createManager() as ProviderManager<C, R>?
        ?: throw RuntimeException("no manager found for '${configuration::class.qualifiedName}'")

@Suppress("UNCHECKED_CAST")
fun <C : ProviderConfiguration, R : ProviderConfigurationRuntime> List<
    ProviderRegistration<*, *, *>,
    >.managerForRuntime(runtime: R): ProviderManager<C, R> =
    this.singleOrNull { it.supportedRuntime == runtime::class }?.createManager()
        as ProviderManager<C, R>?
        ?: throw RuntimeException("no manager found for '${runtime::class.qualifiedName}'")

@Suppress("UNCHECKED_CAST")
fun List<ProviderRegistration<*, *, *>>.managerForConfiguration(configuration: SSHKeyProviderConfiguration): SSHKeyProviderManager<SSHKeyProviderConfiguration, SSHKeyProviderConfigurationRuntime> =
    this.singleOrNull { it.supportedConfiguration == configuration::class }
        ?.createManager()
        as SSHKeyProviderManager<SSHKeyProviderConfiguration, SSHKeyProviderConfigurationRuntime>?
        ?: throw RuntimeException("no manager found for '${configuration::class.qualifiedName}'")
