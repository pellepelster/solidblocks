package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.ProvisionerContext
import kotlin.reflect.KClass

interface ResourceLookupProvider<
    ResourceLookupType : InfrastructureResourceLookup<RuntimeType>,
    RuntimeType,
> {

  suspend fun lookup(lookup: ResourceLookupType, context: ProvisionerContext): RuntimeType?

  suspend fun list(): List<RuntimeType> = TODO("Not yet implemented")

  val supportedLookupType: KClass<*>
}
