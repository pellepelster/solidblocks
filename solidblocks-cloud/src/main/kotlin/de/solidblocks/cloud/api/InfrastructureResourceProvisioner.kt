package de.solidblocks.cloud.api

import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

data class ApplyResult<RuntimeType>(val runtime: RuntimeType?)

data class InfrastructureResourceHelp(val title: String, val help: String)

interface InfrastructureResourceProvisioner<ResourceType, RuntimeType> {

    suspend fun diff(resource: ResourceType, context: ProvisionerContext): ResourceDiff? =
        TODO("Not yet implemented")

    suspend fun apply(
        resource: ResourceType,
        context: ProvisionerContext,
        log: LogContext,
    ): ApplyResult<RuntimeType> = TODO("Not yet implemented")

    suspend fun destroy(
        resource: ResourceType,
        context: ProvisionerContext,
        logContext: LogContext,
    ): Boolean = TODO("Not yet implemented")

    suspend fun destroyAll(context: ProvisionerContext): Boolean = TODO("Not yet implemented")

    suspend fun list(): List<RuntimeType> = TODO("Not yet implemented")

    suspend fun help(
        resource: ResourceType,
        context: ProvisionerContext
    ): List<InfrastructureResourceHelp> = emptyList()

    val supportedResourceType: KClass<*>
}
