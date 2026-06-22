package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.BaseProvisionersRegistry
import de.solidblocks.cloud.api.DestroyableResourceProvisioner
import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.api.ResourceProvisioner
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.lookup.BaseResourceLookupProvider
import de.solidblocks.cloud.api.lookup.ListableResourceLookupProvider
import de.solidblocks.cloud.api.provisioner.BaseResourceProvisioner
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.api.resources.ResourceGroup
import de.solidblocks.cloud.interpolation.EnvironmentVariableInterpolationFactory
import de.solidblocks.cloud.interpolation.StringInterpolationFactory
import de.solidblocks.cloud.interpolation.StringInterpolationRegistry
import de.solidblocks.cloud.providers.ProviderConfiguration
import de.solidblocks.cloud.providers.ProviderConfigurationRuntime
import de.solidblocks.cloud.providers.ProviderManager
import de.solidblocks.cloud.providers.ProviderRegistration
import de.solidblocks.cloud.providers.managerForRuntime
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContextImpl
import de.solidblocks.cloud.provisioner.context.ProvisionerDestroyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.ProvisionerLookupContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.utils.log.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KClass

class ProvisionersRegistry(
    resourceLookupProviders: List<ResourceLookupProvider<*, *>> = emptyList(),
    resourceProvisioners: List<ResourceProvisioner<*, *, *>> = emptyList(),
) : BaseProvisionersRegistry<ProvisionerDiffContext, ProvisionerApplyContext, ProvisionerDestroyContext, ProvisionerLookupContext>(
    (resourceLookupProviders as List<BaseResourceLookupProvider<InfrastructureResourceLookup<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, ProvisionerLookupContext>>),
    @Suppress("UNCHECKED_CAST")
    (resourceProvisioners as List<BaseResourceProvisioner<BaseInfrastructureResource<BaseInfrastructureResourceRuntime>, BaseInfrastructureResourceRuntime, ProvisionerDiffContext, ProvisionerApplyContext>>),
) {
    val interpolationRegistry = StringInterpolationRegistry(this.resourceProvisioners.filterIsInstance<StringInterpolationFactory>() + listOf(EnvironmentVariableInterpolationFactory()))

    companion object {

        fun List<ProviderRegistration<*, *, *>>.createRegistry(providers: List<ProviderConfigurationRuntime>) = ProvisionersRegistry(this.createLookups(providers), this.createProvisioners(providers))

        fun List<ProviderRegistration<*, *, *>>.createProvisioners(providers: List<ProviderConfigurationRuntime>): List<ResourceProvisioner<*, *, *>> = providers.flatMap {
            val manager: ProviderManager<ProviderConfiguration, ProviderConfigurationRuntime> = this.managerForRuntime(it)
            manager.createProvisioners(it)
        }

        fun List<ProviderRegistration<*, *, *>>.createLookups(providers: List<ProviderConfigurationRuntime>) = providers.flatMap {
            val manager: ProviderManager<ProviderConfiguration, ProviderConfigurationRuntime> = this.managerForRuntime(it)
            manager.createLookupProviders(it)
        }
    }
}
