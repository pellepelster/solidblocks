package de.solidblocks.cloud

import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.*
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderConfiguration
import de.solidblocks.cloud.providers.ssh.sshKeyProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createRegistry
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.forService
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.*
import java.io.File
import kotlin.io.path.absolutePathString

class CloudManager(val cloudConfigFile: File) : BaseCloudManager() {

    data class CloudRuntime(
        val configurationContext: CloudConfigurationContext,
        val cloud: CloudConfigurationRuntime,
    )

    fun validate(): Result<CloudRuntime> {
        var log = LogContext.default()
        logInfo(bold("validating cloud configuration '${cloudConfigFile.absolutePath}'"), context = log)
        log = log.indent()

        // parse cloud configuration
        val cloud = when (val result = ConfigurationParser(CloudConfigurationFactory(providerRegistrations, serviceRegistrations)).parse(cloudConfigFile)) {
            is Error -> return Error(result.error)
            is Success<CloudConfiguration> -> result.data
        }
        logInfo("parsed cloud configuration '${cloud.name}'", context = log)

        // ensure no duplicate default providers are registered
        cloud.providers.distinctBy { it.type }.forEach { distinctProvider ->
            if (cloud.providers.count { it.type == distinctProvider.type && it.name == DEFAULT_NAME } > 1) {
                return Error<CloudRuntime>("found more then one default for provider of type '${distinctProvider.type}'. When configuring multiple providers of the same type all non-default providers need a unique name.")
            }
        }

        // ensure no duplicate providers names are registered
        cloud.providers.forEach { provider ->
            if (cloud.providers.count { it.type == provider.type && it.name == provider.name } > 1) {
                return Error<CloudRuntime>("found duplicate provider configuration for type '${provider.type}' with name '${provider.name}'.")
            }
        }

        // ensure no duplicate service names are registered
        cloud.services.forEach { service ->
            if (cloud.services.count { it.name == service.name } > 1) {
                return Error<CloudRuntime>("found duplicate service configuration for name '${service.name}'.")
            }
        }

        // validate that exactly one cloud provider is configured
        if (cloud.providers.count { it is CloudResourceProviderConfiguration } != 1) {
            return Error<CloudRuntime>(
                "more than one or no provider for cloud resource creation found, please register exactly one. available types are: ${
                    providerRegistrations.filter {
                        CloudResourceProviderConfiguration::class.java.isAssignableFrom(
                            it.supportedConfiguration.java,
                        )
                    }.joinToString(", ") { "'${it.type}'" }
                }",
            )
        }

        val sshProviders = cloud.providers.filterIsInstance<SSHKeyProviderConfiguration>()

        // validate that exactly one ssh key provider is configured
        if (sshProviders.count() != 1) {
            return Error<CloudRuntime>(
                "more than one or no provider for ssh keys found (${sshProviders.count()}), please register exactly one. available types are: ${
                    providerRegistrations.filter {
                        SSHKeyProviderConfiguration::class.java.isAssignableFrom(
                            it.supportedConfiguration.java,
                        )
                    }.joinToString(", ") { "'${it.type}'" }
                }",
            )
        }
        val configurationContext = CloudConfigurationContext(cloudConfigFile.toPath().toAbsolutePath().toFile().parentFile.toPath())

        val providers: List<ProviderConfigurtionRuntime> = cloud.providers.map { provider ->
            logInfo("found '${provider.type}' provider with name '${provider.name}'", context = log)
            log = log.indent()

            val manager: ProviderConfigurationManager<ProviderConfiguration, ProviderConfigurtionRuntime> = providerRegistrations.managerForConfiguration(provider)

            logDebug("validating configuration for '${provider.type}' provider '${provider.name}'", context = log)

            val runtime = when (val result = manager.validate(provider, configurationContext, log)) {
                is Error<ProviderConfigurtionRuntime> -> {
                    return Error<CloudRuntime>(result.error)
                }

                is Success<ProviderConfigurtionRuntime> -> {
                    logDebug("configuration for '${provider.type}' provider '${provider.name}' is valid", context = log)
                    result.data
                }
            }

            log = log.unindent()
            runtime
        }

        val registry = this.providerRegistrations.createRegistry(providers)
        val sshKeyProvider = providers.sshKeyProvider()

        val context = ProvisionerContext(
            sshKeyProvider.keyPair,
            sshKeyProvider.privateKey.absolutePathString(),
            configurationContext.configFileDirectory,
            cloud.name,
            cloud.getDefaultEnvironment(),
            registry
        )

        val services: List<ServiceConfigurationRuntime> = cloud.services.mapIndexed { index, service ->
            logInfo("found '${service.type}' service with name '${service.name}'", context = log)
            log = log.indent()

            logDebug("validating configuration for '${service.type}' service '${service.name}'", context = log)

            val manager: ServiceConfigurationManager<ServiceConfiguration, ServiceConfigurationRuntime> = serviceRegistrations.forService(service)

            val runtime = when (val result = manager.validatConfiguration(index, cloud, service, context, log)) {
                is Error<ServiceConfigurationRuntime> -> return Error<CloudRuntime>(result.error)
                is Success<ServiceConfigurationRuntime> -> {
                    logDebug("configuration for '${service.type}' service '${service.name}' is valid", context = log)
                    result.data
                }
            }

            log = log.unindent()
            runtime
        }

        val runtime = CloudRuntime(configurationContext, CloudConfigurationRuntime(cloud.name, cloud.rootDomain, providers, services))
        val cloudProvisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)

        if (runtime.cloud.rootDomain == null) {
            logWarning("no configuration found for '${CloudConfigurationFactory.rootDomain.name}', created services will only be reachable via IP address. Depending on the service this may lead to limited functionality.")
        } else {
            when (val result = cloudProvisioner.provisionerContext.validateDnsZone(runtime.cloud.rootDomain)) {
                is Error<*> -> return Error<CloudRuntime>(result.error)
                else -> {}
            }
        }

        logSuccess("cloud configuration '${cloud.name}' is valid", context = log)
        return Success(runtime)
    }

    fun apply(runtime: CloudRuntime): Result<List<Output>> {
        val log = LogContext.default()
        val cloudProvisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)
        writeSshConfig(runtime)

        when (val result = cloudProvisioner.apply(log)) {
            is Error<Unit> -> return Error<List<Output>>(result.error)
            is Success<*> -> {
                return help(runtime)
            }
        }
    }

    fun writeSshConfig(runtime: CloudRuntime): Result<Unit> {
        val provisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)
        val sshConfigFile = de.solidblocks.cloud.Constants.sshConfigFilePath(runtime.configurationContext.configFileDirectory, runtime.cloud.name)

        when (val result = provisioner.createSSHConfig(sshConfigFile.toFile())) {
            is Error<Unit> -> return result
            is Success<Unit> -> {
                logInfo(bold("ssh config file or cloud '${runtime.cloud.name}' written to '${sshConfigFile.toAbsolutePath()}'"))
                return result
            }
        }
    }

    fun help(runtime: CloudRuntime): Result<List<Output>> {
        val provisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)
        return provisioner.help(runtime)
    }

    fun plan(runtime: CloudRuntime): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val log = LogContext.default()
        val cloudProvisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)
        return cloudProvisioner.plan(log)
    }

}
