package de.solidblocks.cloud.api

import de.solidblocks.cloud.Output
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass


interface InfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    suspend fun diff(resource: ResourceType, context: ProvisionerContext): ResourceDiff? = TODO("Not yet implemented")

    suspend fun apply(resource: ResourceType, context: ProvisionerContext, log: LogContext): Result<RuntimeType> = TODO("Not yet implemented")

    suspend fun destroy(resource: ResourceType, context: ProvisionerContext, logContext: LogContext): Boolean = TODO("Not yet implemented")

    suspend fun destroyAll(context: ProvisionerContext): Boolean = TODO("Not yet implemented")

    suspend fun output(resource: ResourceType, context: ProvisionerContext): List<Output> = emptyList()

    val supportedResourceType: KClass<*>
}
