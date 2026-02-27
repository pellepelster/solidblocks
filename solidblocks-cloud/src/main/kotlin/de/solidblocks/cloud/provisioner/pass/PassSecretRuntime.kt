package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class PassSecretRuntime(val name: String, val secret: String) : BaseInfrastructureResourceRuntime()
