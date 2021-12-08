package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.utils.CustomDataSource
import de.solidblocks.core.Result

class CustomResourceLookupProvider : IResourceLookupProvider<CustomDataSource, String> {

    override fun lookup(datasource: CustomDataSource): Result<String> {
        return Result(datasource.content.invoke())
    }

    override fun getLookupType(): Class<CustomDataSource> {
        return CustomDataSource::class.java
    }
}
