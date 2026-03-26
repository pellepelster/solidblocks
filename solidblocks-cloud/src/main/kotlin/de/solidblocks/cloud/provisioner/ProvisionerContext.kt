package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.security.KeyPair

data class ProvisionerContext(
    val sshKeyPair: KeyPair,
    val sshKeyAbsolutePath: String,
    val sshConfigFilePath: Path,
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

  fun validateDnsZone(domain: String): Result<String> {
    val domainLookup = registry.lookup(HetznerDnsZoneLookup(domain), this)
    return if (domainLookup == null) {
      Error(
          "no zone found for root domain '$domain', please make sure that the zone can be managed by the configured cloud provider",
      )
    } else {
      Success(domain)
    }
  }
}
