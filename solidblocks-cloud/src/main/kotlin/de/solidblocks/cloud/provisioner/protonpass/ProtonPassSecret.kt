package de.solidblocks.cloud.provisioner.protonpass

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.secret.SecretGenerator

class ProtonPassSecret(
    name: String,
    val secretGenerator: SecretGenerator,
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<ProtonPassSecretRuntime>(name, dependsOn) {
    override fun asLookup() = ProtonPassSecretLookup(name)

    override fun logText() = "proton pass secret '$name'"

    override val lookupType = ProtonPassSecretLookup::class
}
