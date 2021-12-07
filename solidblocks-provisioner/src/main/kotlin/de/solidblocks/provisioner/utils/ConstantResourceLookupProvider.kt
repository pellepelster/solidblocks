package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.core.Result
import org.springframework.stereotype.Component

@Component
class ConstantResourceLookupProvider : IResourceLookupProvider<ConstantDataSource, String> {

    override fun lookup(datasource: ConstantDataSource): Result<String> {
        return Result(datasource.content)
    }

    override fun getLookupType(): Class<ConstantDataSource> {
        return ConstantDataSource::class.java
    }
}
