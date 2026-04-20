package de.solidblocks.cloud.api

import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface InfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    suspend fun diff(resource: ResourceType, context: ProvisionerDiffContext): ResourceDiff? = TODO("Not yet implemented")

    suspend fun apply(resource: ResourceType, context: ProvisionerApplyContext, log: LogContext): Result<RuntimeType> = TODO("Not yet implemented")

    suspend fun destroy(resource: ResourceType, context: ProvisionerContext, log: LogContext): Boolean = TODO("Not yet implemented")

    suspend fun destroyAll(context: ProvisionerContext): Boolean = TODO("Not yet implemented")

    val supportedResourceType: KClass<*>
}
