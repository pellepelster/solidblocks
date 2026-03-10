package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class Resource1(name: String) : BaseInfrastructureResource<Resource1Runtime>(name, emptySet()) {

  fun asLookup() = Resource1Lookup(name)

  override val lookupType = Resource1Lookup::class
}
