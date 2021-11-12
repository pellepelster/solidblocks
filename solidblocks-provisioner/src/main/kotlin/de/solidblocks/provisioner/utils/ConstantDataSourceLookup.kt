package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.utils.ConstantDataSource
import de.solidblocks.core.Result
import org.springframework.stereotype.Component

@Component
class ConstantDataSourceLookup : IDataSourceLookup<ConstantDataSource, String> {

    override fun lookup(datasource: ConstantDataSource): Result<String> {
        return Result(datasource, datasource.content)
    }

    override fun getDatasourceType(): Class<ConstantDataSource> {
        return ConstantDataSource::class.java
    }
}
