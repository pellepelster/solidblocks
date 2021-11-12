package de.solidblocks.api.resources.infrastructure.compute

import de.solidblocks.core.IDataSource

class UserDataDataSource(val resourceFile: String, val variables: HashMap<String, IDataSource<String>>) :
    IDataSource<String> {

    override fun name(): String {
        return resourceFile
    }
}
