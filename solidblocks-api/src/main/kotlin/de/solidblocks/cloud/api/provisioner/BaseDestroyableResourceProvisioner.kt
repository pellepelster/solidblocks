package de.solidblocks.cloud.api.provisioner

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

interface BaseDestroyableResourceProvisioner<LookupType : InfrastructureResourceLookup<*>, DestroyContextType> {
    suspend fun destroy(lookup: LookupType, context: DestroyContextType): Boolean
}
