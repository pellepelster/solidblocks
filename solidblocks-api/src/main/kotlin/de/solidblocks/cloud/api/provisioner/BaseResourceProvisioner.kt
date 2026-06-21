package de.solidblocks.cloud.api.provisioner

import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.Result
import kotlin.reflect.KClass

interface BaseResourceProvisioner<ResourceType, RuntimeType, DiffContextType, ApplyContextType> {

    suspend fun diff(resource: ResourceType, context: DiffContextType): Result<ResourceDiff>

    suspend fun apply(resource: ResourceType, context: ApplyContextType): Result<RuntimeType>

    val supportedLookupType: KClass<*>

    val supportedResourceType: KClass<*>
}
