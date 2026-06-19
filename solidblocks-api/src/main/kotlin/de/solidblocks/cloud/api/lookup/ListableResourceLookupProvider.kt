package de.solidblocks.cloud.api.lookup

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

/**
 * Optional capability for lookup providers that can enumerate all existing resources of their type.
 */
interface ListableResourceLookupProvider<LookupType : InfrastructureResourceLookup<*>> {
    suspend fun list(): List<LookupType>
}
