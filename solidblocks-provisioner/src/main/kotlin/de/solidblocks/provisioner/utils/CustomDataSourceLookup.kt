package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.utils.CustomDataSource
import de.solidblocks.core.Result
import org.springframework.stereotype.Component

@Component
class CustomDataSourceLookup : IDataSourceLookup<CustomDataSource, String> {

    override fun lookup(datasource: CustomDataSource): Result<String> {
        return Result(datasource, datasource.content.invoke())
    }

    override fun getDatasourceType(): Class<CustomDataSource> {
        return CustomDataSource::class.java
    }
}
