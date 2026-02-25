package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.api.resources.Resource
import de.solidblocks.cloud.provisioner.ProvisionerContext

data class PassSecret(
    override val name: String,
    val length: Int = 32,
    val allowedChars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9'),
    val secret: ((ProvisionerContext) -> String)? = null,
    override val dependsOn: Set<Resource> = emptySet(),
) : InfrastructureResource<PassSecretRuntime>() {
    fun asLookup() = PassSecretLookup(name)

    override fun logText() = "pass secret '$name'"
}
