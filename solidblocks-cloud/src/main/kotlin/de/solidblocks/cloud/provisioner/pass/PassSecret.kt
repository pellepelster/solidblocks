package de.solidblocks.cloud.provisioner.pass

import de.solidblocks.cloud.Constants.secretPath
import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.provisioner.CloudProvisionerContext

class PassSecret(
    name: String,
    val length: Int = 32,
    val allowedChars: List<Char> = ('A'..'Z') + ('a'..'z') + ('0'..'9'),
    val secret: ((CloudProvisionerContext) -> String)? = null,
    dependsOn: Set<BaseResource> = emptySet(),
) : BaseInfrastructureResource<PassSecretRuntime>(name, dependsOn) {
    fun asLookup() = PassSecretLookup(name)

    override fun logText() = "pass secret '$name'"

    fun shellExportCommand(envName: String) = "export $envName=\"\$(pass $name)\""

    override val lookupType = PassSecretLookup::class
}
