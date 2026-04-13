package de.solidblocks.cloud.api

import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface InfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    suspend fun diff(resource: ResourceType, context: CloudProvisionerContext): ResourceDiff? = TODO("Not yet implemented")

    suspend fun apply(resource: ResourceType, context: CloudProvisionerContext, log: LogContext): Result<RuntimeType> = TODO("Not yet implemented")

    suspend fun destroy(resource: ResourceType, context: CloudProvisionerContext, logContext: LogContext): Boolean = TODO("Not yet implemented")

    suspend fun destroyAll(context: CloudProvisionerContext): Boolean = TODO("Not yet implemented")

    val supportedResourceType: KClass<*>
}
