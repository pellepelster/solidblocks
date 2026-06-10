package de.solidblocks.cloud.services

import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.providers.ProviderCategory
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import kotlin.reflect.KClass

interface ServiceRegistration<C : ServiceConfiguration, R : ServiceConfigurationRuntime> {

    val type: String

    /** Provider categories that must have at least one configured provider for this service to run. */
    val requiredProviders: Set<ProviderCategory> get() = emptySet()

    val supportedConfiguration: KClass<C>
    val supportedRuntime: KClass<R>

    fun createManager(): ServiceManager<C, R>
    fun createFactory(): ConfigurationFactory<C>
}

@Suppress("UNCHECKED_CAST")
fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> List<ServiceRegistration<*, *>>.managerForService(runtime: R): ServiceManager<C, R> =
    this.singleOrNull { it.supportedRuntime == runtime::class }?.createManager()
        as ServiceManager<C, R>?
        ?: throw RuntimeException("no manager found for '${runtime::class.qualifiedName}'")

/**
 * Validates that for every configured service all [ServiceRegistration.requiredProviders]
 * categories have at least one configured provider. Categories are checked in declaration order so
 * the reported violation is deterministic.
 */
fun List<ServiceRegistration<*, *>>.validateProviderRequirements(
    providerRegistrations: List<ProviderRegistration<*, *, *>>,
    services: List<ServiceConfiguration>,
    providers: List<ProviderConfiguration>,
): Result<Unit> {
    services.forEach { service ->
        val registration = this.singleOrNull { it.supportedConfiguration == service::class }
            ?: throw RuntimeException("no service registration found for '${service::class.qualifiedName}'")

        ProviderCategory.entries.filter { it in registration.requiredProviders }.forEach { category ->
            val registrations = providerRegistrations.filter { it.category == category }
            val configTypes = registrations.map { it.supportedConfiguration }.toSet()

            if (providers.none { it::class in configTypes }) {
                val types = registrations.joinToString(", ") { "'${it.type}'" }
                return Error(
                    "service '${service.name}' needs a '${category.name}' provider, available types are: $types",
                )
            }
        }
    }

    return Success(Unit)
}
