package de.solidblocks.cloud

import aws.smithy.kotlin.runtime.util.type
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
import de.solidblocks.utils.*
import java.io.File
import java.nio.file.Path

class CloudManager(val cloudConfigFile: File) : BaseCloudManager() {

    fun validate(): Result<CloudConfigurationRuntime> {
        val log = LogContext.default()
        log.info(bold("validating cloud configuration '${cloudConfigFile.absolutePath}'"))
        val validateLog = log.indent()

        val cloud = when (val result = parseConfiguration(validateLog)) {
            is Error<CloudConfiguration> -> return Error(result.error)
            is Success<CloudConfiguration> -> result.data
        }

        when (val result = validateNoDuplicateProviders(cloud)) {
            is Error<Unit> -> return Error(result.error)
            is Success<Unit> -> {}
        }

        when (val result = validateNoDuplicateServices(cloud)) {
            is Error<Unit> -> return Error(result.error)
            is Success<Unit> -> {}
        }

        when (val result = providerRegistrations.validateCardinality(cloud.providers)) {
            is Error<Unit> -> return Error(result.error)
            is Success<Unit> -> {}
        }

        val configurationContext =
            CloudConfigurationContext(
                EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
                cloudConfigFile.toPath().toAbsolutePath().toFile().parentFile.toPath(),
            )

        val providers = when (val result = buildProviderRuntimes(cloud, configurationContext, validateLog)) {
            is Error<List<ProviderConfigurationRuntime>> -> return Error(result.error)
            is Success<List<ProviderConfigurationRuntime>> -> result.data
        }

        when (val result = validateServicePrerequisites(cloud)) {
            is Error<Unit> -> return Error(result.error)
            is Success<Unit> -> {}
        }

        val registry = this.providerRegistrations.createRegistry(providers)
        val context = ValidationContextImpl(
            EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
            registry,
            serviceRegistrations,
        )

        val services = when (val result = buildServiceRuntimes(cloud, context, validateLog)) {
            is Error<List<ServiceConfigurationRuntime>> -> return Error(result.error)
            is Success<List<ServiceConfigurationRuntime>> -> result.data
        }

        val runtime =
            CloudConfigurationRuntime(
                configurationContext,
                EnvironmentContext(cloud.name, DEFAULT_ENVIRONMENT),
                cloud.environmentVariables,
                cloud.rootDomain,
                providers,
                services,
            )

        when (val result = validateDnsZone(runtime)) {
            is Error<Unit> -> return Error(result.error)
            is Success<Unit> -> {}
        }

        logSuccess("cloud configuration '${cloud.name}' is valid", context = validateLog)
        return Success(runtime)
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

    private fun buildProviderRuntimes(
        cloud: CloudConfiguration,
        configurationContext: CloudConfigurationContext,
        log: LogContext,
    ): Result<List<ProviderConfigurationRuntime>> {
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

    private fun validateServicePrerequisites(cloud: CloudConfiguration): Result<Unit> {
        cloud.services.forEach { service ->
            service.neededProviders.forEach { neededProvider ->
                if (cloud.providers.filterIsInstance(neededProvider.java).count() == 0) {
                    val types = providerRegistrations.filter { neededProvider == it.supportedConfiguration.java }.map { it.type }
                    if (types.isNotEmpty()) {
                        return Error("service '${service.name}' needs the following provider type(s) ${types.joinToString(", ") { "'$it'" }}")
                    }

                    val categoryTypes = providerRegistrations.filter { neededProvider.java.isAssignableFrom(it.supportedConfiguration.java) }.map { it.type }
                    if (categoryTypes.isNotEmpty()) {
                        return Error("service '${service.name}' needs one the following provider types ${categoryTypes.joinToString(", ") { "'$it'" }}")
                    }

                    throw RuntimeException("failed to resolve prerequisites for service '${service.name}'")
                }
            }
        }

        return Success(Unit)
    }

    private fun buildServiceRuntimes(
        cloud: CloudConfiguration,
        context: ValidationContextImpl,
        log: LogContext,
    ): Result<List<ServiceConfigurationRuntime>> {
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

    private fun validateDnsZone(runtime: CloudConfigurationRuntime): Result<Unit> {
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use {
            if (runtime.rootDomain == null) {
                logWarning(
                    "no configuration found for '${CloudConfigurationFactory.rootDomain.name}', created services will only be reachable via IP address. Depending on the service this may lead to limited functionality.",
                )
            } else {
                val lookup = it.context.lookup(HetznerDnsZoneLookup(runtime.rootDomain))

                if (lookup == null) {
                    return Error(
                        "no zone found for root domain '${runtime.rootDomain}', please make sure that the zone can be managed by the configured cloud provider",
                    )
                }
            }

            return Success(Unit)
        }
    }

    private fun <T> withProvisioner(runtime: CloudConfigurationRuntime, block: (CloudProvisioner) -> T): T =
        CloudProvisioner(runtime, serviceRegistrations, providerRegistrations).use(block)

    fun apply(runtime: CloudConfigurationRuntime, tainSecrets: Boolean): Result<String> {
        val log = LogContext.default()
        return withProvisioner(runtime) { provisioner ->
            when (val result = provisioner.apply(tainSecrets, log)) {
                is Error<Unit> -> {
                    writeSshConfig(provisioner)
                    Error(result.error)
                }

                is Success<*> -> {
                    writeSshConfig(provisioner)
                    info(provisioner)
                }
            }
        }
    }

    // TODO integration test for generated ssh config
    fun writeSshConfig(runtime: CloudConfigurationRuntime): Result<Unit> =
        withProvisioner(runtime) { writeSshConfig(it) }

    private fun writeSshConfig(provisioner: CloudProvisioner): Result<Unit> =
        when (val result = provisioner.createSSHConfig()) {
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

    fun info(runtime: CloudConfigurationRuntime): Result<String> =
        withProvisioner(runtime) { info(it) }

    private fun info(provisioner: CloudProvisioner): Result<String> =
        provisioner.info(provisioner.runtime)

    fun status(runtime: CloudConfigurationRuntime): Result<String> =
        withProvisioner(runtime) { it.status(runtime) }

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

    fun infoJson(runtime: CloudConfigurationRuntime): Result<CloudInfo> =
        withProvisioner(runtime) { it.infoJson(runtime) }

    fun plan(runtime: CloudConfigurationRuntime): Result<Map<ResourceGroup, List<ResourceDiff>>> {
        val log = LogContext.default()
        return withProvisioner(runtime) { it.plan({ false }, log) }
    }
}
