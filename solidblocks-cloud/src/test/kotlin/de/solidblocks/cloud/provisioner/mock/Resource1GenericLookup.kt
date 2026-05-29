package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class Resource1GenericLookup(name: String) : InfrastructureResourceLookup<Resource1Runtime>(name, emptySet())
