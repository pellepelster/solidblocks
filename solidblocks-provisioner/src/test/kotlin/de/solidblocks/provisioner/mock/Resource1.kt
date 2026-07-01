package de.solidblocks.provisioner.mock

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource

class Resource1(
    name: String,
    val diffBehaviour: DiffBehaviour,
    dependsOn: Set<BaseResource> = emptySet(),
    val applyBehaviour: ApplyBehaviour = ApplyBehaviour.succeed,
    taintable: Boolean = true,
    taintRequiresRecreate: Boolean = false,
) : BaseInfrastructureResource<Resource1Runtime>(name, dependsOn, taintable, taintRequiresRecreate) {

    override fun asLookup() = Resource1Lookup(name)

    override val lookupType = Resource1Lookup::class
}
