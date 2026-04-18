package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.DEFAULT_ENVIRONMENT
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.providers.CloudResourceProviderConfiguration
import de.solidblocks.cloud.providers.DEFAULT_NAME
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.providers.managerForConfiguration
import de.solidblocks.cloud.providers.types.backup.BackupProviderConfiguration
import de.solidblocks.cloud.providers.types.secret.SecretProviderConfiguration
import de.solidblocks.cloud.providers.types.ssh.SSHKeyProviderConfiguration
import de.solidblocks.cloud.providers.types.ssh.sshKeyProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createRegistry
import de.solidblocks.cloud.services.CloudInfo
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.forService
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import de.solidblocks.utils.bold
import de.solidblocks.utils.logDebug
import de.solidblocks.utils.logInfo
import de.solidblocks.utils.logSuccess
import de.solidblocks.utils.logWarning
import java.io.File
import kotlin.io.path.absolutePathString

class CloudManager(val cloudConfigFile: File) : BaseCloudManager() {

    fun validate(): Result<CloudConfigurationRuntime> {
        var log = LogContext.default()
        log.info(bold("validating cloud configuration '${cloudConfigFile.absolutePath}'"))
        log = log.indent()

        // parse cloud configuration
        val cloud =
            when (
                val result =
                    ConfigurationParser(
                        CloudConfigurationFactory(providerRegistrations, serviceRegistrations),
                    )
                        .parse(cloudConfigFile)
            ) {
                is Error -> return Error(result.error)
                is Success<CloudConfiguration> -> result.data
            }
        log.info("parsed cloud configuration '${cloud.name}'")

        // ensure no duplicate default providers are registered
        cloud.providers
            .distinctBy { it.type }
            .forEach { distinctProvider ->
                if (
                    cloud.providers.count {
                        it.type == distinctProvider.type && it.name == DEFAULT_NAME
                    } > 1
                ) {
                    return@validate Error<CloudConfigurationRuntime>(
                        "found more then one default for provider of type '${distinctProvider.type}'. When configuring multiple providers of the same type all non-default providers need a unique name.",
                    )
                }
            }

        // ensure no duplicate providers names are registered
        cloud.providers.forEach { provider ->
            if (cloud.providers.count { it.type == provider.type && it.name == provider.name } > 1) {
                return Error<CloudConfigurationRuntime>(
                    "found duplicate provider configuration for type '${provider.type}' with name '${provider.name}'.",
                )
            }
        }

        // ensure no duplicate service names are registered
        cloud.services.forEach { service ->
            if (cloud.services.count { it.name == service.name } > 1) {
                return Error<CloudConfigurationRuntime>(
                    "found duplicate service configuration for name '${service.name}'.",
                )
            }
        }

        /** validate that exactly one cloud provider is configured */
        val cloudProviders = cloud.providers.filterIsInstance<CloudResourceProviderConfiguration>()
        if (cloudProviders.count() != 1) {
            return Error<CloudConfigurationRuntime>(
                "more than one or no cloud provider found (${cloudProviders.count()}), please register exactly one. available types are: ${
                    providerRegistrations.filter {
                        CloudResourceProviderConfiguration::class.java.isAssignableFrom(
                            it.supportedConfiguration.java,
                        )
                    }.joinToString(", ") { "'${it.type}'" }
                }",
            )
        }

        /** validate that exactly one ssh key provider is configured */
        val sshProviders = cloud.providers.filterIsInstance<SSHKeyProviderConfiguration>()
        if (sshProviders.count() != 1) {
            return Error<CloudConfigurationRuntime>(
                "more than one or no provider for ssh keys found (${sshProviders.count()}), please register exactly one. available types are: ${
                    providerRegistrations.filter {
                        SSHKeyProviderConfiguration::class.java.isAssignableFrom(
                            it.supportedConfiguration.java,
                        )
                    }.joinToString(", ") { "'${it.type}'" }
                }",
            )
        }

        /** validate that exactly one backup provider is configured */
        val backupProviders = cloud.providers.filterIsInstance<BackupProviderConfiguration>()
        if (backupProviders.count() != 1) {
            return Error<CloudConfigurationRuntime>(
                "more than one or no provider for backups found (${backupProviders.count()}), please register exactly one. available types are: ${
                    providerRegistrations.filter {
                        BackupProviderConfiguration::class.java.isAssignableFrom(
                            it.supportedConfiguration.java,
                        )
                    }.joinToString(", ") { "'${it.type}'" }
                }",
            )
        }

        /** validate that exactly one secret provider is configured */
        val secretProviders = cloud.providers.filterIsInstance<SecretProviderConfiguration>()
        if (secretProviders.count() != 1) {
            return Error<CloudConfigurationRuntime>(
                "more than one or no secret provider found (${secretProviders.count()}), please register exactly one. available types are: ${
                    providerRegistrations.filter {
                        SecretProviderConfiguration::class.java.isAssignableFrom(
                            it.supportedConfiguration.java,
                        )
                    }.joinToString(", ") { "'${it.type}'" }
                }",
            )
        }

        val configurationContext =
            CloudConfigurationContext(
                EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
                cloudConfigFile.toPath().toAbsolutePath().toFile().parentFile.toPath(),
            )

        val providers: List<ProviderConfigurationRuntime> =
            cloud.providers.map { provider ->
                log.info("found '${provider.type}' provider with name '${provider.name}'")
                log = log.indent()

                val manager:
                    ProviderManager<ProviderConfiguration, ProviderConfigurationRuntime> =
                    providerRegistrations.managerForConfiguration(provider)

                logDebug(
                    "validating configuration for '${provider.type}' provider '${provider.name}'",
                    context = log,
                )

                val runtime =
                    when (val result = manager.validateConfiguration(provider, configurationContext, log)) {
                        is Error<ProviderConfigurationRuntime> -> {
                            return Error<CloudConfigurationRuntime>(result.error)
                        }

                        is Success<ProviderConfigurationRuntime> -> {
                            logDebug(
                                "configuration for '${provider.type}' provider '${provider.name}' is valid",
                                context = log,
                            )
                            result.data
                        }
                    }

                log = log.unindent()
                runtime
            }

        val registry = this.providerRegistrations.createRegistry(providers)
        val sshKeyProvider = providers.sshKeyProvider()

        ProvisionerContext(
            sshKeyProvider.keyPair,
            sshKeyProvider.privateKey.absolutePathString(),
            configurationContext.configFileDirectory,
            EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
            registry,
            serviceRegistrations,
        )
            .use {
                val services: List<ServiceConfigurationRuntime> =
                    cloud.services.mapIndexed { index, service ->
                        log.info("found '${service.type}' service with name '${service.name}'")
                        log = log.indent()

                        logDebug(
                            "validating configuration for '${service.type}' service '${service.name}'",
                            context = log,
                        )

                        val manager: ServiceManager<ServiceConfiguration, ServiceConfigurationRuntime> =
                            serviceRegistrations.forService(service)

                        val runtime =
                            when (
                                val result = manager.validateConfiguration(index, cloud, service, it, log)
                            ) {
                                is Error<ServiceConfigurationRuntime> ->
                                    return Error<CloudConfigurationRuntime>(result.error)

                                is Success<ServiceConfigurationRuntime> -> {
                                    logDebug(
                                        "configuration for '${service.type}' service '${service.name}' is valid",
                                        context = log,
                                    )
                                    result.data
                                }
                            }

                        log = log.unindent()
                        runtime
                    }

                val runtime =
                    CloudConfigurationRuntime(
                        configurationContext,
                        EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
                        cloud.rootDomain,
                        providers,
                        services,
                    )
                CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
                    if (runtime.rootDomain == null) {
                        logWarning(
                            "no configuration found for '${CloudConfigurationFactory.rootDomain.name}', created services will only be reachable via IP address. Depending on the service this may lead to limited functionality.",
                        )
                    } else {
                        when (val result = it.context.validateDnsZone(runtime.rootDomain)) {
                            is Error<*> -> return Error<CloudConfigurationRuntime>(result.error)
                            else -> {}
                        }
                    }

                    logSuccess("cloud configuration '${cloud.name}' is valid", context = log)
                    return Success(runtime)
                }
            }
    }

    fun apply(runtime: CloudConfigurationRuntime): Result<String> {
        val log = LogContext.default()
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            when (val result = it.apply(log)) {
                is Error<Unit> -> {
                    writeSshConfig(runtime)
                    return Error<String>(result.error)
                }

                is Success<*> -> {
                    writeSshConfig(runtime)
                    return info(runtime)
                }
            }
        }
    }

    // TODO integration test for generated ssh config
    fun writeSshConfig(runtime: CloudConfigurationRuntime): Result<Unit> {
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            when (val result = it.createSSHConfig()) {
                is Error<String> -> return Error<Unit>(result.error)
                is Success<String> -> {
                    logInfo(
                        bold(
                            "ssh config file for cloud '${runtime.environment.cloud}' written to '${result.data}', use 'ssh -F ${result.data} <host>' to access the VMs",
                        ),
                    )
                    return Success(Unit)
                }
            }
        }
    }

    fun info(runtime: CloudConfigurationRuntime): Result<String> {
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            return it.info(runtime)
        }
    }

    fun status(runtime: CloudConfigurationRuntime): Result<String> {
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            return it.status(runtime)
        }
    }

    fun infoJson(runtime: CloudConfigurationRuntime): Result<CloudInfo> {
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            return it.infoJson(runtime)
        }
    }

    fun plan(runtime: CloudConfigurationRuntime): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val log = LogContext.default()
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            return it.plan(log)
        }
    }
}
