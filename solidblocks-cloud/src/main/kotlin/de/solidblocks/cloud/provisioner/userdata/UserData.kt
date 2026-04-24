package de.solidblocks.cloud.provisioner.userdata

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.ensureLookup
import de.solidblocks.cloud.services.DefaultServerResources
import de.solidblocks.cloudinit.ServiceUserData
import de.solidblocks.shell.toCloudInit
import java.util.*

class UserDataRuntime(val userData: String, val ephemeralUserData: String) : BaseInfrastructureResourceRuntime()

data class UserDataResult(val userData: String, val ephemeralUserData: String)

class UserData(dependsOn: Set<BaseInfrastructureResource<*>>, val block: ((ProvisionerContext) -> UserDataResult?)) : InfrastructureResourceLookup<UserDataRuntime>(UUID.randomUUID().toString(), dependsOn)

fun ServiceUserData.toResult(context: ProvisionerContext, defaultResources: DefaultServerResources): UserDataResult {
    val script = this.shellScript()
        .toCloudInit(
            context.ensureLookup(defaultResources.sshIdentityRsaSecret.asLookup()).secret,
            context.ensureLookup(defaultResources.sshIdentityED25519Secret.asLookup()).secret,
        ).render()

    val ephemeralScript = this.ephemeralScript()
        .toCloudInit(
            context.ensureLookup(defaultResources.sshIdentityRsaSecret.asLookup()).secret,
            context.ensureLookup(defaultResources.sshIdentityED25519Secret.asLookup()).secret,
        ).render()

    return UserDataResult(script, ephemeralScript)
}
