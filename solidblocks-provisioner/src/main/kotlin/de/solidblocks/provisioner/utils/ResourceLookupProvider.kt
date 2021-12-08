package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.utils.ResourceLookup
import de.solidblocks.provisioner.Provisioner

class ResourceLookupProvider<RuntimeType>(private val provisioner: Provisioner) :
    IResourceLookupProvider<ResourceLookup<RuntimeType>, String> {

    override fun lookup(lookup: ResourceLookup<RuntimeType>): de.solidblocks.core.Result<String> {
        return this.provisioner.lookup(lookup.resource).mapResult {
            lookup.call(it as RuntimeType)
        }
    }

    override fun getLookupType(): Class<*> {
        return ResourceLookup::class.java
    }
}
