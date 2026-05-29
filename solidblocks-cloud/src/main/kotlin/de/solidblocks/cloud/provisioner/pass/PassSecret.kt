package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.secret.SecretGenerator


class PassSecret(
    name: String,
    val secretGenerator: SecretGenerator,
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<PassSecretRuntime>(name, dependsOn) {
    override fun asLookup() = PassSecretLookup(name)

    override fun logText() = "pass secret '$name'"

    fun shellExportCommand(envName: String) = "export $envName=\"\$(pass $name)\""

    override val lookupType = PassSecretLookup::class
}
