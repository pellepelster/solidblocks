package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.InfrastructureResource

data class Resource1(override val name: String) :
    InfrastructureResource<Resource1Runtime>() {

  fun asLookup() = Resource1Lookup(name)
}
