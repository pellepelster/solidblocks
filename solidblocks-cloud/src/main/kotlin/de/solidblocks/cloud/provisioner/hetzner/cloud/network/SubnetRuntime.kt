package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.LabeledInfrastructureResourceRuntime

data class SubnetRuntime(
    val subnet: String,
    val network: NetworkLookup,
) : LabeledInfrastructureResourceRuntime {

  override val labels: Map<String, String>
    get() = emptyMap()
}
