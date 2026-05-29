package de.solidblocks.cloud.provisioner.protonpass

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class ProtonPassSecretLookup(name: String) : InfrastructureResourceLookup<ProtonPassSecretRuntime>(name, emptySet()) {
    override fun logText() = "proton pass secret '$name'"
}
