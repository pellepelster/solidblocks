package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import de.solidblocks.cloud.utils.Result
import de.solidblocks.utils.LogContext
import kotlin.reflect.KClass

interface InfrastructureResourceProvisioner<ResourceType : BaseResource, RuntimeType : BaseInfrastructureResourceRuntime, LookupType : InfrastructureResourceLookup<*>> {

    suspend fun diff(resource: ResourceType, context: ProvisionerDiffContext): ResourceDiff? = TODO("Not yet implemented")

    suspend fun apply(resource: ResourceType, context: ProvisionerApplyContext, log: LogContext): Result<RuntimeType> = TODO("Not yet implemented")

    suspend fun destroy(lookup: LookupType, context: SSHProvisionerContext, log: LogContext): Boolean = TODO("Not yet implemented")

    suspend fun destroyAll(context: ProvisionerContext): Boolean = TODO("Not yet implemented")

    fun convertGenericResource(resource: BaseResource): BaseResource = TODO("Not yet implemented")

    val lookupType: KClass<*>

    val resourceType: KClass<*>

    val genericResourceType: KClass<*>?
        get() = null

    val genericLookupType: KClass<*>?
}
