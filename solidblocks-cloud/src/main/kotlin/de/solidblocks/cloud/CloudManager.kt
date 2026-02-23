package de.solidblocks.cloud

import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.providers.*
import de.solidblocks.cloud.providers.hetzner.HetznerProviderRegistration
import de.solidblocks.cloud.providers.pass.PassProviderRegistration
import de.solidblocks.cloud.providers.ssh.SSHKeyProviderConfiguration
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderRegistration
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.DnsZoneLookup
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationManager
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.forService
import de.solidblocks.cloud.services.s3.S3ServiceRegistration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.*
import java.io.File

class CloudManager(val cloudConfigFile: File) {

    val serviceRegistrations = listOf(S3ServiceRegistration())

    val providerRegistrations =
        listOf(
            HetznerProviderRegistration(),
            PassProviderRegistration(),
            LocalSSHKeyProviderRegistration(),
        )

    data class CloudRuntime(
        val configuration: CloudConfiguration,
        val providers: List<ProviderRuntime>,
        val services: List<ServiceConfigurationRuntime>,
    )

    fun validate(): Result<CloudRuntime> {
        var log = LogContext.default()
        logInfo(bold("validating cloud configuration '${cloudConfigFile.absolutePath}'"), context = log)

        val cloudConfiguration =
            when (
                val result =
                    ConfigurationParser(
                        CloudConfigurationFactory(providerRegistrations, serviceRegistrations),
                    )
                        .parse(
                            cloudConfigFile,
                        )
            ) {
                is Error -> {
                    return Error(result.error)
                }

                is Success<CloudConfiguration> -> result.data
            }

        logInfo("parsed cloud configuration '${cloudConfiguration.name}'", context = log)

        cloudConfiguration.providers
            .distinctBy { it.type }
            .forEach { distinctProvider ->
                if (
                    cloudConfiguration.providers.count {
                        it.type == distinctProvider.type && it.name == DEFAULT_NAME
                    } > 1
                ) {
                    return Error<CloudRuntime>(
                        "found more then one default for provider of type '${distinctProvider.type}'. When configuring multiple providers of the same type all non-default providers need a unique name.",
                    )
                }
            }

        cloudConfiguration.providers.forEach { provider ->
            if (
                cloudConfiguration.providers.count {
                    it.type == provider.type && it.name == provider.name
                } > 1
            ) {
                return Error<CloudRuntime>(
                    "found duplicate provider configuration for type '${provider.type}' with name '${provider.name}'.",
                )
            }
        }

        // validate that exactly one cloud provider is configured
        if (cloudConfiguration.providers.count { it is CloudResourceProviderConfiguration } != 1) {
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

        val sshProviders = cloudConfiguration.providers.filterIsInstance<SSHKeyProviderConfiguration>()

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

        val providers: List<ProviderRuntime> =
            cloudConfiguration.providers.map { provider ->
                logInfo("found '${provider.type}' provider with name '${provider.name}'", context = log)
                log = log.indent()

                val manager: ProviderConfigurationManager<ProviderConfiguration, ProviderRuntime> =
                    providerRegistrations.managerForConfiguration(provider)

                logDebug(
                    "validating configuration for '${provider.type}' provider '${provider.name}'",
                    context = log,
                )
                val context = ConfigurationContext(cloudConfigFile.parentFile.toPath().toAbsolutePath())
                val runtime =
                    when (val result = manager.validate(provider, log, context)) {
                        is Error<ProviderRuntime> -> {
                            return Error<CloudRuntime>(result.error)
                        }

                        is Success<ProviderRuntime> -> {
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

        val services: List<ServiceConfigurationRuntime> =
            cloudConfiguration.services.map { service ->
                logInfo("found '${service.type}' service with name '${service.name}'", context = log)
                log = log.indent()

                logDebug(
                    "validating configuration for '${service.type}' service '${service.name}'",
                    context = log,
                )
                val manager:
                        ServiceConfigurationManager<ServiceConfiguration, ServiceConfigurationRuntime> =
                    serviceRegistrations.forService(service, cloudConfiguration)

                val runtime =
                    when (val result = manager.validatConfiguration(service, log)) {
                        is Error<ServiceConfigurationRuntime> -> return Error<CloudRuntime>(result.error)
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

        val runtime = CloudRuntime(cloudConfiguration, providers, services)
        val cloudProvisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)

        val rootDomain =
            cloudProvisioner.provisionerContext.lookup(DnsZoneLookup(runtime.configuration.rootDomain))
                ?: return Error<CloudRuntime>(
                    "no zone found for root domain '${runtime.configuration.rootDomain}'",
                )

        logSuccess("cloud configuration '${cloudConfiguration.name}' is valid", context = log)
        return Success(runtime)
    }

    fun apply(runtime: CloudRuntime): Result<Unit> {
        val log = LogContext.default()

        val cloudProvisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)
        val result = cloudProvisioner.apply(log)

        return result
    }

    fun plan(runtime: CloudRuntime): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val log = LogContext.default()
        val cloudProvisioner = CloudProvisioner(runtime, serviceRegistrations, providerRegistrations)
        return cloudProvisioner.plan(log)
    }
}
