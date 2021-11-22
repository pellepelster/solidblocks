package de.solidblocks.api.resources.infrastructure.utils

import de.solidblocks.core.IResourceLookup

class ResourceLookup<RuntimeType>(val resource: IResourceLookup<*>, val call: (RuntimeType) -> String) : IResourceLookup<String> {
    override fun name(): String {
        return "resource.lookup.${resource.name()}"
    }
}
