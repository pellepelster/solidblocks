package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretRuntime

class GenericSecretLookup(name: String) : InfrastructureResourceLookup<PassSecretRuntime>(name, emptySet()) {
    override fun logText() = "secret '$name'"
}
