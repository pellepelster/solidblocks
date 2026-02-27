package de.solidblocks.cloud.provisioner.userdata

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.ProvisionerContext
import java.util.*

class UserDataRuntime(val userData: String) : BaseInfrastructureResourceRuntime()

class UserData(
    dependsOn: Set<BaseInfrastructureResource<*>>,
    val block: ((ProvisionerContext) -> String?),
) : InfrastructureResourceLookup<UserDataRuntime>(UUID.randomUUID().toString(), dependsOn)
