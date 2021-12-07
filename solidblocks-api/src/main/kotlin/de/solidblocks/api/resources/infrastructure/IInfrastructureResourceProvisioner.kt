package de.solidblocks.api.resources.infrastructure

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.core.Result

interface IInfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    fun diff(resource: ResourceType): Result<ResourceDiff> = TODO("Not yet implemented")
    fun apply(resource: ResourceType): Result<*> = TODO("Not yet implemented")

    fun destroy(resource: ResourceType): Boolean = TODO("Not yet implemented")
    fun destroyAll(): Boolean = TODO("Not yet implemented")

    fun getResourceType(): Class<*>
}
