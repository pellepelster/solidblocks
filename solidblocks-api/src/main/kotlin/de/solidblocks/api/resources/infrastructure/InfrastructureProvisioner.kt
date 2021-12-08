package de.solidblocks.api.resources.infrastructure

import de.solidblocks.core.IResourceLookup
import de.solidblocks.core.Result

interface InfrastructureProvisioner {
    fun <LookupType : IResourceLookup<RuntimeType>, RuntimeType> lookup(resource: LookupType): Result<RuntimeType>
}
