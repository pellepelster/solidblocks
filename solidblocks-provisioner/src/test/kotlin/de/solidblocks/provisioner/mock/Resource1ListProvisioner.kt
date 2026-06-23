package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.lookup.ListableResourceLookupProvider

class Resource1ListProvisioner(
    val items: List<Resource1Lookup> = emptyList(),
) : TestResourceLookupProvider<Resource1Lookup, Resource1Runtime>,
    ListableResourceLookupProvider<Resource1Lookup> {

    override suspend fun lookup(lookup: Resource1Lookup, context: TestLookupContext) = items.firstOrNull { it.name == lookup.name }?.let { Resource1Runtime(it.name) }

    override suspend fun list(): List<Resource1Lookup> = items

    override val supportedLookupType = Resource1Lookup::class
}
