package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class Resource2Lookup(name: String) :
    InfrastructureResourceLookup<Resource2Runtime>(name, emptySet()) {
  override fun logText() = "custom log text '$name'"
}
