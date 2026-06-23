package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import de.solidblocks.provisioner.*
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource1Provisioner :
    TestResourceLookupProvider<Resource1Lookup, Resource1Runtime>,
    TestResourceProvisioner<Resource1, Resource1Runtime, Resource1Lookup> {

    private val logger = KotlinLogging.logger {}

    val resources = mutableMapOf<String, Resource1>()

    override suspend fun lookup(lookup: Resource1Lookup, context: TestLookupContext) = resources[lookup.name]?.let { Resource1Runtime(lookup.name) }

    override suspend fun diff(resource: Resource1, context: TestDiffContext): Result<ResourceDiff> = Success(
        lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
            ?: ResourceDiff(resource, missing),
    )

    override suspend fun apply(resource: Resource1, context: TestApplyContext): Result<Resource1Runtime> {
        resources[resource.name] = resource

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<Resource1Runtime>("creation error")
    }

    override val supportedLookupType = Resource1Lookup::class

    override val supportedResourceType = Resource1::class
}
