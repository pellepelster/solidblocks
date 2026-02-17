package de.solidblocks.cloud.provisioner.mock

import de.solidblocks.cloud.api.endpoint.Endpoint
import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime

data class Resource1Runtime(val name: String, val endpoints: List<Endpoint>) :
    InfrastructureResourceRuntime {
  override fun endpoints() = endpoints
}
