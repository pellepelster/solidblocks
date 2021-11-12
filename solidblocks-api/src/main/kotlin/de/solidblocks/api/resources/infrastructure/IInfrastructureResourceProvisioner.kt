package de.solidblocks.api.resources.infrastructure

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result

interface IInfrastructureResourceProvisioner<ResourceType : IInfrastructureResource<RuntimeType>, RuntimeType> {

    fun lookup(resource: ResourceType): Result<RuntimeType>
    fun diff(resource: ResourceType): Result<ResourceDiff<RuntimeType>>
    fun apply(resource: ResourceType): Result<*>

    fun destroy(resource: ResourceType): Result<*> = Result<Any>(resource)
    fun destroyAll(): Result<*> = Result<Any>(NullResource)

    fun getResourceType(): Class<ResourceType>
}
