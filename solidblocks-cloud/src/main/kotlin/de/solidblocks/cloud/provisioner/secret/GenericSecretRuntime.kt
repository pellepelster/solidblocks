package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

open class GenericSecretRuntime(val name: String, val secret: String) : BaseInfrastructureResourceRuntime()
