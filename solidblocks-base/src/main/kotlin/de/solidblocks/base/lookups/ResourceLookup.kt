package de.solidblocks.base.lookups

import de.solidblocks.core.IResourceLookup

class ResourceLookup<RuntimeType>(val resource: IResourceLookup<*>, val call: (RuntimeType) -> String) :
    IResourceLookup<String> {
    override val name = "resource.lookup.${resource.name}"
}
