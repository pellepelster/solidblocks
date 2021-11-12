package de.solidblocks.api.resources.infrastructure

import de.solidblocks.core.IDataSource
import de.solidblocks.core.Result

interface IDataSourceLookup<DataSourceType : IDataSource<T>, T> {

    fun lookup(datasource: DataSourceType): Result<T>

    fun getDatasourceType(): Class<*>
}
