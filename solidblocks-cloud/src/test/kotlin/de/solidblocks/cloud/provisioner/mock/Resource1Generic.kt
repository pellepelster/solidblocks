package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class Resource1Generic(name: String) : BaseInfrastructureResource<Resource1Runtime>(name, emptySet()) {

    override fun asLookup() = Resource1GenericLookup(name)

    override val lookupType = Resource1GenericLookup::class
}
