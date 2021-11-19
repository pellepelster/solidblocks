package de.solidblocks.api.resources.infrastructure

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result

interface IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    fun lookup(resource: ResourceType): Result<RuntimeType>
    fun diff(resource: ResourceType): Result<ResourceDiff>
    fun apply(resource: ResourceType): Result<*>

    fun destroy(resource: ResourceType): Result<*> = Result<Any>(NullResource)
    fun destroyAll(): Result<*> = Result<Any>(NullResource)

    fun getResourceType(): Class<*>
}
