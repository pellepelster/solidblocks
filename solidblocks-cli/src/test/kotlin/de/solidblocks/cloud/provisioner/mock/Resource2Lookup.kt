package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.ResourceLookup

data class Resource2Lookup(override val name: String) : ResourceLookup<Resource2Runtime> {
  override fun logText() = "custom log text '$name'"
}
