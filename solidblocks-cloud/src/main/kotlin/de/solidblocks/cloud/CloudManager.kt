package de.solidblocks.cloud

import de.solidblocks.cloud.Constants.DEFAULT_ENVIRONMENT
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceGroup
import de.solidblocks.cloud.configuration.ConfigurationParser
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.configuration.model.CloudConfigurationFactory
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.providers.*
import de.solidblocks.cloud.provisioner.ProvisionersRegistry.Companion.createRegistry
import de.solidblocks.cloud.provisioner.context.ValidationContextImpl
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.fold
import de.solidblocks.cloud.utils.result
import de.solidblocks.utils.*
import java.io.Closeable
import java.io.File
import java.nio.file.Path

class CloudManager(val cloudConfigFile: File) : BaseCloudManager(), Closeable {

    private val provisioners = mutableMapOf<CloudConfigurationRuntime, CloudProvisioner>()

    private fun provisionerFor(runtime: CloudConfigurationRuntime): CloudProvisioner = provisioners.getOrPut(runtime) { CloudProvisioner(runtime, serviceRegistrations, providerRegistrations) }

    override fun close() {
        provisioners.values.forEach(CloudProvisioner::close)
        provisioners.clear()
    }

    fun validate(): Result<CloudConfigurationRuntime> = result {
        val log = LogContext.default()
        log.info(bold("validating cloud configuration '${cloudConfigFile.absolutePath}'"))
        val validateLog = log.indent()

        val cloud = parseConfiguration(validateLog).bind()
        validateNoDuplicateProviders(cloud).bind()
        validateNoDuplicateServices(cloud).bind()
        providerRegistrations.validateCardinality(cloud.providers).bind()
        serviceRegistrations.validateProviderRequirements(providerRegistrations, cloud.services, cloud.providers).bind()

        val configurationContext =
            CloudConfigurationContext(
                EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
                cloudConfigFile.toPath().toAbsolutePath().toFile().parentFile.toPath(),
            )

        val providers = buildProviderRuntimes(cloud, configurationContext, validateLog).bind()

        val registry = providerRegistrations.createRegistry(providers)
        val context = ValidationContextImpl(
            EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
            registry,
            serviceRegistrations,
        )

        val services = buildServiceRuntimes(cloud, context, validateLog).bind()

        val runtime =
            CloudConfigurationRuntime(
                configurationContext,
                EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
                cloud.environmentVariables,
                cloud.rootDomain,
                providers,
                services,
            )

        withProvisioner(runtime) { it.validateWiring() }.bind()
        validateDnsZone(runtime).bind()

        logSuccess("cloud configuration '${cloud.name}' is valid", context = validateLog)
        runtime
    }

    private fun parseConfiguration(log: LogContext): Result<CloudConfiguration> {
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
        return Success(cloud)
    }

    private fun validateNoDuplicateProviders(cloud: CloudConfiguration): Result<Unit> {
        // ensure no duplicate default providers are registered
        cloud.providers
            .distinctBy { it.type }
            .forEach { distinctProvider ->
                if (
                    cloud.providers.count {
                        it.type == distinctProvider.type && it.name == DEFAULT_NAME
                    } > 1
                ) {
                    return Error(
                        "found more then one default for provider of type '${distinctProvider.type}'. When configuring multiple providers of the same type all non-default providers need a unique name.",
                    )
                }
            }

        // ensure no duplicate providers names are registered
        cloud.providers.forEach { provider ->
            if (cloud.providers.count { it.type == provider.type && it.name == provider.name } > 1) {
                return Error(
                    "found duplicate provider configuration for type '${provider.type}' with name '${provider.name}'.",
                )
            }
        }

        return Success(Unit)
    }

    private fun validateNoDuplicateServices(cloud: CloudConfiguration): Result<Unit> {
        cloud.services.forEach { service ->
            if (cloud.services.count { it.name == service.name } > 1) {
                return Error(
                    "found duplicate service configuration for name '${service.name}'.",
                )
            }
        }

        return Success(Unit)
    }

    private fun buildProviderRuntimes(cloud: CloudConfiguration, configurationContext: CloudConfigurationContext, log: LogContext): Result<List<ProviderConfigurationRuntime>> {
        val providers = cloud.providers.map { provider ->
            log.info("found '${provider.type}' provider with name '${provider.name}'")
            val providerLog = log.indent()

            val manager:
                ProviderManager<ProviderConfiguration, ProviderConfigurationRuntime> =
                providerRegistrations.managerForConfiguration(provider)

            providerLog.debug(
                "validating configuration for '${provider.type}' provider '${provider.name}'",
            )

            when (val result = manager.validateConfiguration(provider, configurationContext, providerLog)) {
                is Error<ProviderConfigurationRuntime> -> return Error(result.error)

                is Success<ProviderConfigurationRuntime> -> {
                    providerLog.debug(
                        "configuration for '${provider.type}' provider '${provider.name}' is valid",
                    )
                    result.data
                }
            }
        }

        return Success(providers)
    }

    private fun buildServiceRuntimes(cloud: CloudConfiguration, context: ValidationContextImpl, log: LogContext): Result<List<ServiceConfigurationRuntime>> {
        val services = cloud.services.mapIndexed { index, service ->
            log.info("found '${service.type}' service with name '${service.name}'")
            val serviceLog = log.indent()

            serviceLog.debug(
                "validating configuration for '${service.type}' service '${service.name}'",
            )

            val manager: ServiceManager<ServiceConfiguration, ServiceConfigurationRuntime> =
                serviceRegistrations.forService(service)

            when (
                val result = manager.validateConfiguration(index, cloud, service, context, serviceLog)
            ) {
                is Error<ServiceConfigurationRuntime> -> return Error(result.error)

                is Success<ServiceConfigurationRuntime> -> {
                    serviceLog.debug(
                        "configuration for '${service.type}' service '${service.name}' is valid",
                    )
                    result.data
                }
            }
        }

        return Success(services)
    }

    private fun validateDnsZone(runtime: CloudConfigurationRuntime): Result<Unit> = withProvisioner(runtime) {
        if (runtime.rootDomain == null) {
            logWarning(
                "no configuration found for '${CloudConfigurationFactory.rootDomain.name}', created services will only be reachable via IP address. Depending on the service this may lead to limited functionality.",
            )
            Success(Unit)
        } else {
            val lookup = it.context.lookup(HetznerDnsZoneLookup(runtime.rootDomain))

            if (lookup == null) {
                Error(
                    "no zone found for root domain '${runtime.rootDomain}', please make sure that the zone can be managed by the configured cloud provider",
                )
            } else {
                Success(Unit)
            }
        }
    }

    private fun <T> withProvisioner(runtime: CloudConfigurationRuntime, block: (CloudProvisioner) -> T): T = block(provisionerFor(runtime))

    fun apply(runtime: CloudConfigurationRuntime, tainSecrets: Boolean): Result<String> {
        val log = LogContext.default()
        return withProvisioner(runtime) { provisioner ->
            val applyResult = provisioner.apply(tainSecrets, log)
            writeSshConfig(provisioner)
            applyResult.fold(
                onError = { Error(it.error, it.cause) },
                onSuccess = { info(provisioner) },
            )
        }
    }

    // TODO integration test for generated ssh config
    fun writeSshConfig(runtime: CloudConfigurationRuntime): Result<Unit> = withProvisioner(runtime) { writeSshConfig(it) }

    private fun writeSshConfig(provisioner: CloudProvisioner): Result<Unit> = when (val result = provisioner.createSSHConfig()) {
        is Error<Path> -> Error(result.error)
        is Success<Path> -> {
            val path = Path.of(".").toAbsolutePath().relativize(result.data)
            logInfo(
                bold(
                    "ssh config file for cloud '${provisioner.runtime.environmentContext.cloud}' written to '$path', use 'ssh -F $path <host>' to access the VMs",
                ),
            )
            Success(Unit)
        }
    }

    fun info(runtime: CloudConfigurationRuntime): Result<String> = withProvisioner(runtime) { info(it) }

    private fun info(provisioner: CloudProvisioner): Result<String> = provisioner.info(provisioner.runtime)

    fun status(runtime: CloudConfigurationRuntime): Result<String> = withProvisioner(runtime) { it.status(runtime) }

    fun maintenance(runtime: CloudConfigurationRuntime): Result<Unit> {
        val log = LogContext()
        log.info(bold("running maintenance"))

        return withProvisioner(runtime) {
            it.maintenance(runtime, log).also {
                if (it is Success<Unit>) {
                    log.success("maintenance finished")
                }
            }
        }
    }

    fun debugInterpolation(runtime: CloudConfigurationRuntime, interpolated: String): Result<Unit> {
        val log = LogContext()
        log.info(bold("debugging interpolation for '$interpolated'"))

        return withProvisioner(runtime) {
            when (val result = it.registry.interpolationRegistry.validate(interpolated)) {
                is Error<Unit> -> Error("error validating '$interpolated': ${result.error}")
                is Success<*> -> {
                    when (val resolved = it.registry.interpolationRegistry.resolve(interpolated)) {
                        is Error<String> -> Error("error resolving '$interpolated': ${resolved.error}")
                        is Success<String> -> {
                            log.success("'$interpolated' resolved to '${resolved.data}'")
                            Success(Unit)
                        }
                    }
                }
            }
        }
    }

    fun infoJson(runtime: CloudConfigurationRuntime): Result<CloudInfo> = withProvisioner(runtime) { it.infoJson(runtime) }

    fun plan(runtime: CloudConfigurationRuntime): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val log = LogContext.default()
        return withProvisioner(runtime) { it.plan({ false }, log) }
    }
}
