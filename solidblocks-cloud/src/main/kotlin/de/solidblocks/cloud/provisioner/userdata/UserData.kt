package de.solidblocks.cloud.provisioner.userdata

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.ResourceLookup
import de.solidblocks.cloud.provisioner.ProvisionerContext
import java.util.*

data class UserDataRuntime(val userData: String) : InfrastructureResourceRuntime

data class UserData(
    override val dependsOn: Set<InfrastructureResource<*, *>>,
    val block: ((ProvisionerContext) -> String?),
) : ResourceLookup<UserDataRuntime> {

  override val name = UUID.randomUUID().toString()
}
