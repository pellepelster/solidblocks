package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime

data class NetworkRuntime(val id: String) : LabeledInfrastructureResourceRuntime {
  override val labels: Map<String, String>
    get() = emptyMap()
}
