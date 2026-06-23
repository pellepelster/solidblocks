package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffItem
import de.solidblocks.cloud.api.diff.ResourceDiffStatus
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.*
import de.solidblocks.cloud.api.resources.DestroyableResourceProvisioner
import de.solidblocks.provisioner.*
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource2Provisioner :
    TestResourceLookupProvider<Resource2Lookup, Resource2Runtime>,
    TestResourceProvisioner<Resource2, Resource2Runtime, Resource2Lookup>,
    DestroyableResourceProvisioner<Resource2Lookup, TestDestroyContext> {

    private val logger = KotlinLogging.logger {}

    val resources = mutableMapOf<String, Resource2>()

    override suspend fun lookup(lookup: Resource2Lookup, context: TestLookupContext) = resources[lookup.name]?.let { Resource2Runtime(lookup.name) }

    override suspend fun diff(resource: Resource2, context: TestDiffContext): Result<ResourceDiff> = when (resource.diffBehaviour) {
        DiffBehaviour.error_on_diff -> Error("diff error for ${resource.logText()}")
        DiffBehaviour.unknown_on_diff -> Success(
            ResourceDiff(
                resource,
                ResourceDiffStatus.unknown,
            ),
        )

        DiffBehaviour.throw_exception_on_diff -> throw RuntimeException()
        DiffBehaviour.duplicate_on_diff -> Success(
            ResourceDiff(
                resource,
                ResourceDiffStatus.duplicate,
                duplicateErrorMessage = "duplicate error for ${resource.logText()}",
            ),
        )

        DiffBehaviour.force_recreate_change -> Success(
            ResourceDiff(
                resource,
                has_changes,
                changes = listOf(ResourceDiffItem("force_recreate_change", triggersRecreate = true)),
            ),
        )

        DiffBehaviour.up_to_date_or_missing -> Success(
            lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
                ?: ResourceDiff(resource, missing),
        )
    }

    override suspend fun apply(resource: Resource2, context: TestApplyContext): Result<Resource2Runtime> {
        when (resource.applyBehaviour) {
            ApplyBehaviour.error_on_apply -> return Error("apply error for ${resource.logText()}")
            ApplyBehaviour.throw_exception_on_apply -> throw RuntimeException("apply exception for ${resource.logText()}")
            ApplyBehaviour.succeed -> {}
        }

        appliedResources.add(resource.name)
        resources[resource.name] = resource

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<Resource2Runtime>("creation error")
    }

    val destroyedResources = mutableListOf<String>()

    val appliedResources = mutableListOf<String>()

    var destroyResult = true

    fun isDestroyed(name: String) = destroyedResources.contains(name)

    fun isApplied(name: String) = appliedResources.contains(name)

    fun applyCount(name: String) = appliedResources.count { it == name }

    override suspend fun destroy(lookup: Resource2Lookup, context: TestDestroyContext): Boolean {
        destroyedResources.add(lookup.name)
        return destroyResult
    }

    override val supportedLookupType = Resource2Lookup::class

    override val supportedResourceType = Resource2::class
}
