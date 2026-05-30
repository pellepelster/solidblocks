package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource

enum class DiffBehaviour {
    error_on_diff,
    unknown_on_diff,
    throw_exception_on_diff,
    force_recreate_change,
    up_to_date_or_missing,
}

class Resource2(name: String, val diffBehaviour: DiffBehaviour, dependsOn: Set<BaseResource> = emptySet()) : BaseInfrastructureResource<Resource1Runtime>(name, dependsOn) {

    override fun asLookup() = Resource2Lookup(name)

    override val lookupType = Resource2Lookup::class
}
