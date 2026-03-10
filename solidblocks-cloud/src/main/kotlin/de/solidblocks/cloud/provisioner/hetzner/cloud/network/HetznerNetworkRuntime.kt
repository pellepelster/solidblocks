package de.solidblocks.cloud.provisioner.hetzner.cloud.network

import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResourceRuntime

class HetznerNetworkRuntime(val id: Long, val name: String, val deleteProtected: Boolean, labels: Map<String, String>) : BaseLabeledInfrastructureResourceRuntime(labels)