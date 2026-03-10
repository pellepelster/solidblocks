package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource

class Resource2(name: String, dependsOn: Set<BaseResource> = emptySet()) :
    BaseInfrastructureResource<Resource1Runtime>(name, dependsOn) {

  fun asLookup() = Resource2Lookup(name)

  override val lookupType = Resource2Lookup::class
}
