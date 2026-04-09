package de.solidblocks.cloud.lookups

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.ProvisionerContext
import java.util.UUID

class StringLookup(val block: (ProvisionerContext) -> String) : InfrastructureResourceLookup<String>(UUID.randomUUID().toString(), emptySet()) {
    override fun logText() = "static lookup '$name'"
}
