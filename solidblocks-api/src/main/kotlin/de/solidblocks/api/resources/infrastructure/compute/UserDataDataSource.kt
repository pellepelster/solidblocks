package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.core.IResourceLookup

class UserDataDataSource(val resourceFile: String, val variables: HashMap<String, IResourceLookup<String>>) :
    IResourceLookup<String> {

    override fun name(): String {
        return resourceFile
    }
}
