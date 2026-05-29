package de.solidblocks.cloud.provisioner.secret

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class GenericSecretLookup(name: String) : InfrastructureResourceLookup<GenericSecretRuntime>(name, emptySet()) {
    override fun logText() = "secret '$name'"
}
