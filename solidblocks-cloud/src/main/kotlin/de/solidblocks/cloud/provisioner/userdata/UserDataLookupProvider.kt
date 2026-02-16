package de.solidblocks.cloud.provisioner.userdata

import de.solidblocks.cloud.api.ResourceLookupProvider
import de.solidblocks.cloud.provisioner.ProvisionerContext
import io.github.oshai.kotlinlogging.KotlinLogging

class UserDataLookupProvider : ResourceLookupProvider<UserData, UserDataRuntime> {

  private val logger = KotlinLogging.logger {}

  override suspend fun lookup(lookup: UserData, context: ProvisionerContext): UserDataRuntime? {
    val s = lookup.block.invoke(context)
    return UserDataRuntime(s)
  }

  override val supportedLookupType = UserData::class
}
