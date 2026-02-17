package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.ResourceLookup

data class Resource1Lookup(override val name: String) : ResourceLookup<Resource1Runtime>
