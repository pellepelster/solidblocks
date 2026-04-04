package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.sshConfigFilePath
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
import de.solidblocks.cloud.secret.SecretProviderConfiguration
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceInfo
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.forService
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.*
import java.io.File
import kotlin.io.path.absolutePathString

class CloudManager(val cloudConfigFile: File) : BaseCloudManager() {

  fun validate(): Result<CloudConfigurationRuntime> {
    var log = LogContext.default()
    logInfo(bold("validating cloud configuration '${cloudConfigFile.absolutePath}'"), context = log)
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
    logInfo("parsed cloud configuration '${cloud.name}'", context = log)

    // ensure no duplicate default providers are registered
    cloud.providers
        .distinctBy { it.type }
        .forEach { distinctProvider ->
          if (
              cloud.providers.count {
                it.type == distinctProvider.type && it.name == DEFAULT_NAME
              } > 1
          ) {
            return Error<CloudConfigurationRuntime>(
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
            cloudConfigFile.toPath().toAbsolutePath().toFile().parentFile.toPath(),
        )

    val providers: List<ProviderConfigurtionRuntime> =
        cloud.providers.map { provider ->
          logInfo("found '${provider.type}' provider with name '${provider.name}'", context = log)
          log = log.indent()

          val manager:
              ProviderConfigurationManager<ProviderConfiguration, ProviderConfigurtionRuntime> =
              providerRegistrations.managerForConfiguration(provider)

          logDebug(
              "validating configuration for '${provider.type}' provider '${provider.name}'",
              context = log,
          )

          val runtime =
              when (val result = manager.validate(provider, configurationContext, log)) {
                is Error<ProviderConfigurtionRuntime> -> {
                  return Error<CloudConfigurationRuntime>(result.error)
                }

                is Success<ProviderConfigurtionRuntime> -> {
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
            cloud.name,
            cloud.getDefaultEnvironment(),
            registry,
        )
        .use {
          val services: List<ServiceConfigurationRuntime> =
              cloud.services.mapIndexed { index, service ->
                logInfo(
                    "found '${service.type}' service with name '${service.name}'",
                    context = log,
                )
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
                  cloud.name,
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

  fun apply(runtime: CloudConfigurationRuntime): Result<List<Output>> {
    val log = LogContext.default()
    CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
      when (val result = it.apply(log)) {
        is Error<Unit> -> return Error<List<Output>>(result.error)
        is Success<*> -> {
          writeSshConfig(runtime)
          return help(runtime)
        }
      }
    }
  }

  fun writeSshConfig(runtime: CloudConfigurationRuntime): Result<Unit> {
    CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
      val sshConfigFile =
          sshConfigFilePath(
              runtime.context.configFileDirectory,
              runtime.name,
          )

      when (val result = it.createSSHConfig(sshConfigFile.toFile())) {
        is Error<Unit> -> return result
        is Success<Unit> -> {
          logInfo(
              bold(
                  "ssh config file for cloud '${runtime.name}' written to '${sshConfigFile.toAbsolutePath()}', use 'ssh -F ${sshConfigFile.toAbsolutePath()} <host>' to access the VMs",
              ),
          )
          return result
        }
      }
    }
  }

  fun help(runtime: CloudConfigurationRuntime): Result<List<Output>> {
    CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
      return it.help(runtime)
    }
  }

  fun plan(runtime: CloudConfigurationRuntime): Result<Map<ResourceGroup, List<ResourceDiff>>> {
    val log = LogContext.default()
    CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
      return it.plan(log)
    }
  }

  fun info(runtime: CloudConfigurationRuntime): Result<List<ServiceInfo>> {
    val log = LogContext.default()
    CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
      return it.info(log)
    }
  }
}
