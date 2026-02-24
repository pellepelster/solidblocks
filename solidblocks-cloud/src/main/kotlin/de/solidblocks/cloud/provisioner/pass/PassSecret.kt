package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.InfrastructureResource

data class PassSecret(
    override val name: String,
    val length: Int = 32,
    val allowedChars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9'),
) : InfrastructureResource<PassSecret, PassSecretRuntime>() {
    fun asLookup() = PassSecretLookup(name)

    override fun logText() = "pass secret '$name'"
}
