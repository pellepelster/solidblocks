package de.solidblocks.cloud.api.lookup

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import kotlin.reflect.KClass

interface BaseResourceLookupProvider<ResourceLookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime, LookupContextType> {

    suspend fun lookup(lookup: ResourceLookupType, context: LookupContextType): RuntimeType?

    val supportedLookupType: KClass<*>
}
