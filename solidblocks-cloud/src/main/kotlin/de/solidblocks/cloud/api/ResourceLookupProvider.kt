package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import kotlin.reflect.KClass

interface ResourceLookupProvider<
    ResourceLookupType : InfrastructureResourceLookup<RuntimeType>,
    RuntimeType,
> {

  suspend fun lookup(lookup: ResourceLookupType, context: CloudProvisionerContext): RuntimeType?

  suspend fun list(): List<RuntimeType> = TODO("Not yet implemented")

  val supportedLookupType: KClass<*>
}
