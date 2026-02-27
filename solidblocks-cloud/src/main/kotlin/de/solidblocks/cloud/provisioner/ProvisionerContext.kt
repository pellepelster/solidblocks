package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.KeyPair

data class ProvisionerContext(
    val sshKeyPair: KeyPair,
    val cloudName: String,
    val environmentName: String,
    val registry: ProvisionersRegistry,
) {
  private val logger = KotlinLogging.logger {}

  fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(
      lookup: ResourceLookupType
  ): RuntimeType? = registry.lookup(lookup, this)

  fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> ensureLookup(
      lookup: ResourceLookupType
  ): RuntimeType =
      registry.lookup(lookup, this).let {
        if (it == null) {
          logger.error { "could not find resource ${lookup.logText()}" }
        }
        it!!
      }
}
