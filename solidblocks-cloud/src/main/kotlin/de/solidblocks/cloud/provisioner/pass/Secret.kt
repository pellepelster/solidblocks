package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.InfrastructureResource

data class Secret(
    override val name: String,
    val length: Int = 32,
    val allowedChars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9'),
) : InfrastructureResource<Secret, SecretRuntime>() {
  fun asLookup() = SecretLookup(name)
}
