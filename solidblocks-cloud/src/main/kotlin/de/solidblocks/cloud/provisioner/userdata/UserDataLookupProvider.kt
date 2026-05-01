package de.solidblocks.cloud.provisioner.userdata

import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.provisioner.context.SSHProvisionerContext
import io.github.oshai.kotlinlogging.KotlinLogging

class UserDataLookupProvider : ResourceLookupProvider<UserData, UserDataRuntime> {

    private val logger = KotlinLogging.logger {}

    override suspend fun lookup(lookup: UserData, context: SSHProvisionerContext) = lookup.block.invoke(context)?.let { UserDataRuntime(it.userData, it.ephemeralUserData) }

    override val supportedLookupType = UserData::class
}
