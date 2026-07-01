package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.diff.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.EndpointResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class EndpointResourceRuntimeImpl(val name: String, override val endpoints: List<Endpoint>) :
    BaseInfrastructureResourceRuntime(), EndpointResourceRuntime

class EndpointResourceLookup(name: String) : InfrastructureResourceLookup<EndpointResourceRuntimeImpl>(name, emptySet())

class EndpointResource(
    name: String,
    val endpoints: List<Endpoint>,
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<EndpointResourceRuntimeImpl>(name, dependsOn) {

    override fun asLookup() = EndpointResourceLookup(name)

    override val lookupType = EndpointResourceLookup::class
}

class EndpointResourceProvisioner :
    TestResourceLookupProvider<EndpointResourceLookup, EndpointResourceRuntimeImpl>,
    TestResourceProvisioner<EndpointResource, EndpointResourceRuntimeImpl, EndpointResourceLookup> {

    val resources = mutableMapOf<String, EndpointResource>()

    override suspend fun lookup(lookup: EndpointResourceLookup, context: TestLookupContext) =
        resources[lookup.name]?.let { EndpointResourceRuntimeImpl(lookup.name, it.endpoints) }

    override suspend fun diff(resource: EndpointResource, context: TestDiffContext): Result<ResourceDiff> = Success(
        lookup(resource.asLookup(), context)?.let { ResourceDiff(resource, up_to_date) }
            ?: ResourceDiff(resource, missing),
    )

    override suspend fun apply(resource: EndpointResource, context: TestApplyContext): Result<EndpointResourceRuntimeImpl> {
        resources[resource.name] = resource

        return lookup(resource.asLookup(), context)?.let { Success(it) }
            ?: Error<EndpointResourceRuntimeImpl>("creation error")
    }

    override val supportedLookupType = EndpointResourceLookup::class

    override val supportedResourceType = EndpointResource::class
}
