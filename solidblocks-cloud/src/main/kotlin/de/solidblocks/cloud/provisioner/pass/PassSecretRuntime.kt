package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime

data class PassSecretRuntime(val name: String, val secret: String) : InfrastructureResourceRuntime
