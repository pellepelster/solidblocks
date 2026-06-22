package de.solidblocks.cloud.api.provisioner

import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.diff.ResourceDiff
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import kotlin.reflect.KClass

interface BaseResourceProvisioner<
        ResourceType : BaseInfrastructureResource<RuntimeType>,
        RuntimeType : BaseInfrastructureResourceRuntime,
        DiffContextType,
        ApplyContextType
        > {

    suspend fun diff(resource: ResourceType, context: DiffContextType): Result<ResourceDiff>

    suspend fun apply(resource: ResourceType, context: ApplyContextType): Result<RuntimeType>

    val supportedLookupType: KClass<*>

    val supportedResourceType: KClass<*>
}
