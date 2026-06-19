package de.solidblocks.cloud.api.provisioner

interface BaseDestroyableResourceProvisioner<LookupType, DestroyContextType> {
    suspend fun destroy(lookup: LookupType, context: DestroyContextType): Boolean
}
