package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffItem
import de.solidblocks.cloud.api.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.InfrastructureResourceLookupProvider
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource2Provisioner :
    InfrastructureResourceLookupProvider<Resource2Lookup, Resource2Runtime>,
    InfrastructureResourceProvisioner<Resource2, Resource2Runtime, Resource2Lookup> {

    private val logger = KotlinLogging.logger {}

    val resources = mutableMapOf<String, Resource2>()

    override suspend fun lookup(lookup: Resource2Lookup, context: SSHProvisionerContext) = resources[lookup.name]?.let { Resource2Runtime(lookup.name) }

    override suspend fun diff(resource: Resource2, context: ProvisionerDiffContext): ResourceDiff? = if (resource.name == "throw_exception_on_diff") {
        throw RuntimeException()
    } else if (resource.name == "force_recreate_change") {
        ResourceDiff(
            resource,
            has_changes,
            changes = listOf(ResourceDiffItem("force_recreate_change", triggersRecreate = true)),
        )
    } else {
        lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
            ?: ResourceDiff(resource, missing)
    }

    override suspend fun apply(resource: Resource2, context: ProvisionerApplyContext, log: LogContext): Result<Resource2Runtime> {
        appliedResources.add(resource.name)
        resources[resource.name] = resource

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<Resource2Runtime>("creation error")
    }

    val destroyedResources = mutableListOf<String>()

    val appliedResources = mutableListOf<String>()

    fun isDestroyed(name: String) = destroyedResources.contains(name)

    fun isApplied(name: String) = appliedResources.contains(name)

    override suspend fun destroy(lookup: Resource2Lookup, context: SSHProvisionerContext, log: LogContext): Boolean {
        destroyedResources.add(lookup.name)
        return true
    }

    override val lookupType = Resource2Lookup::class

    override val resourceType = Resource2::class
}
