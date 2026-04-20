package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.context.ProvisionerContext

sealed class SecretGenerator() {
    abstract fun generate(context: ProvisionerContext): String
    abstract fun isEphemeral(): Boolean
}

class RandomSecret(
    val length: Int = 32,
    val allowedChars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9'),
) : SecretGenerator() {
    override fun generate(context: ProvisionerContext) = (1..length).map { allowedChars.random() }.joinToString("")
    override fun isEphemeral() = true
}

class OneTimeGeneratedSecret(val secret: (ProvisionerContext) -> String) : SecretGenerator() {
    override fun generate(context: ProvisionerContext) = secret.invoke(context)
    override fun isEphemeral() = true
}

class StaticSecret(val secret: (ProvisionerContext) -> String) : SecretGenerator() {
    override fun generate(context: ProvisionerContext) = secret.invoke(context)
    override fun isEphemeral() = false
}

class PassSecret(
    name: String,
    val secretGenerator: SecretGenerator,
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<PassSecretRuntime>(name, dependsOn) {
    fun asLookup() = PassSecretLookup(name)

    override fun logText() = "pass secret '$name'"

    fun shellExportCommand(envName: String) = "export $envName=\"\$(pass $name)\""

    override val lookupType = PassSecretLookup::class
}
