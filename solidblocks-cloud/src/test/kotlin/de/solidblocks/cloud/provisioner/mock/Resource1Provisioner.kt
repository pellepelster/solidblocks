package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.InfrastructureResourceProvisioner
import de.solidblocks.cloud.api.ResourceDiff
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging

class Resource1Provisioner :
    ResourceLookupProvider<Resource1Lookup, Resource1Runtime>,
    InfrastructureResourceProvisioner<Resource1, Resource1Runtime> {

    private val logger = KotlinLogging.logger {}

    val resources = mutableMapOf<String, Resource1>()

    override suspend fun lookup(lookup: Resource1Lookup, context: CloudProvisionerContext) = resources[lookup.name]?.let { Resource1Runtime(lookup.name, listOf()) }

    override suspend fun diff(resource: Resource1, context: CloudProvisionerContext) = lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
        ?: ResourceDiff(resource, missing)

    override suspend fun apply(resource: Resource1, context: CloudProvisionerContext, log: LogContext): Result<Resource1Runtime> {
        resources[resource.name] = resource

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<Resource1Runtime>("creation error")
    }

    override val supportedLookupType = Resource1Lookup::class

    override val supportedResourceType = Resource1::class
}
