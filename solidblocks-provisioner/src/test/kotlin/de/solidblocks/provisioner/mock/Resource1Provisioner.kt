package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffItem
import de.solidblocks.cloud.api.diff.ResourceDiffStatus
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.has_changes
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource1Provisioner :
    TestResourceLookupProvider<Resource1Lookup, Resource1Runtime>,
    TestResourceProvisioner<Resource1, Resource1Runtime, Resource1Lookup> {

    private val logger = KotlinLogging.logger {}

    val resources = mutableMapOf<String, Resource1>()

    override suspend fun lookup(lookup: Resource1Lookup, context: TestLookupContext) = resources[lookup.name]?.let { Resource1Runtime(lookup.name) }

    override suspend fun diff(resource: Resource1, context: TestDiffContext): Result<ResourceDiff> = when (resource.diffBehaviour) {
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

        DiffBehaviour.change_no_recreate -> Success(
            ResourceDiff(
                resource,
                has_changes,
                changes = listOf(ResourceDiffItem("change_no_recreate", changed = true)),
            ),
        )

        DiffBehaviour.parent_missing_on_diff -> Success(
            ResourceDiff(resource, ResourceDiffStatus.parent_missing),
        )

        DiffBehaviour.up_to_date_or_missing -> Success(
            lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
                ?: ResourceDiff(resource, missing),
        )
    }

    override suspend fun apply(resource: Resource1, context: TestApplyContext): Result<Resource1Runtime> {
        when (resource.applyBehaviour) {
            ApplyBehaviour.error_on_apply -> return Error("apply error for ${resource.logText()}")
            ApplyBehaviour.throw_exception_on_apply -> throw RuntimeException("apply exception for ${resource.logText()}")
            ApplyBehaviour.succeed -> {}
        }

        appliedResources.add(resource.name)
        resources[resource.name] = resource

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<Resource1Runtime>("creation error")
    }

    val appliedResources = mutableListOf<String>()

    fun isApplied(name: String) = appliedResources.contains(name)

    fun applyCount(name: String) = appliedResources.count { it == name }

    override val supportedLookupType = Resource1Lookup::class

    override val supportedResourceType = Resource1::class

}
