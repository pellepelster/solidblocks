package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import kotlin.reflect.KClass

interface InfrastructureResourceLookupProvider<ResourceLookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> {

    suspend fun lookup(lookup: ResourceLookupType, context: SSHProvisionerContext): RuntimeType?

    suspend fun list(): List<ResourceLookupType> = TODO("Not yet implemented")

    val supportedLookupType: KClass<*>
}
