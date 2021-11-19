package de.solidblocks.provisioner.utils

import de.solidblocks.api.resources.infrastructure.IDataSourceLookup
import de.solidblocks.api.resources.infrastructure.utils.ResourceLookup
import de.solidblocks.core.IResource
import de.solidblocks.core.Result
import de.solidblocks.provisioner.Provisioner
import org.springframework.stereotype.Component

@Component
class ResourceDataSourceLookup<RuntimeType>(private val provisioner: Provisioner) :
    IDataSourceLookup<ResourceLookup<RuntimeType>, String> {

    override fun lookup(datasource: ResourceLookup<RuntimeType>): Result<String> {
        return this.provisioner.lookup<IResource, Any>(datasource.resource).mapResourceResult {
            datasource.call(it as RuntimeType)
        }
    }

    override fun getDatasourceType(): Class<ResourceLookup<*>> {
        return ResourceLookup::class.java
    }
}
