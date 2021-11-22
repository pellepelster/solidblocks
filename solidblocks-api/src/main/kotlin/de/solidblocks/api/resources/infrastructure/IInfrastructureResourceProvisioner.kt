package de.solidblocks.api.resources.infrastructure

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.core.NullResource
import de.solidblocks.core.Result

interface IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    fun diff(resource: ResourceType): Result<ResourceDiff> = TODO("Not yet implemented")
    fun apply(resource: ResourceType): Result<*> = TODO("Not yet implemented")

    fun destroy(resource: ResourceType): Result<*> = Result<Any>(NullResource)
    fun destroyAll(): Result<*> = Result<Any>(NullResource)

    fun getResourceType(): Class<*>

}
