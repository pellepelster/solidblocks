package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import kotlin.reflect.KClass

interface InfrastructureResourceLookupProvider<ResourceLookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType> {

    suspend fun lookup(lookup: ResourceLookupType, context: SSHProvisionerContext): RuntimeType?

    suspend fun list(): List<ResourceLookupType> = TODO("Not yet implemented")

    fun convertGenericLookup(lookup: InfrastructureResourceLookup<*>): BaseResource = TODO("Not yet implemented")

    val lookupType: KClass<*>

    val genericLookupType: KClass<*>?
        get() = null
}
