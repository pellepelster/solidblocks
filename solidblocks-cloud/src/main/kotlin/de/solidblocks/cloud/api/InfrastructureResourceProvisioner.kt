package de.solidblocks.cloud.api

import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface InfrastructureResourceProvisioner<ResourceType, RuntimeType, LookupType> {

    suspend fun diff(resource: ResourceType, context: ProvisionerDiffContext): Result<ResourceDiff>

    suspend fun apply(resource: ResourceType, context: ProvisionerApplyContext, log: LogContext): Result<RuntimeType>

    val supportedLookupType: KClass<*>

    val supportedResourceType: KClass<*>
}
