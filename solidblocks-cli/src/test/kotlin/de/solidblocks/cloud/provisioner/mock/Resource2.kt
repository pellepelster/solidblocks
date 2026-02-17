package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.InfrastructureResource

data class Resource2(
    override val name: String,
    override val dependsOn: Set<InfrastructureResource<*, *>> = emptySet(),
) : InfrastructureResource<Resource2, Resource1Runtime>() {

  fun asLookup() = Resource2Lookup(name)
}
