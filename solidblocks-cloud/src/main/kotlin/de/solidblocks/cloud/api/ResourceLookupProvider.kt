package de.solidblocks.cloud.api

import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.ProvisionerContext
import kotlin.reflect.KClass

interface ResourceLookupProvider<ResourceLookupType : ResourceLookup<RuntimeType>, RuntimeType> {
  suspend fun lookup(lookup: ResourceLookupType, context: ProvisionerContext): RuntimeType?

  val supportedLookupType: KClass<*>
}
