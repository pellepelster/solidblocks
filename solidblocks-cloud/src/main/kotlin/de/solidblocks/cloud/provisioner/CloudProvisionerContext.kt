package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.SSHClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.core.Closeable
import java.nio.file.Path
import java.security.KeyPair
import kotlin.reflect.KClass

interface CloudProvisionerContext {
  val sshKeyPair: KeyPair
  val sshConfigFilePath: Path
  val cloudName: String

  fun validateDnsZone(zone: String): Result<String>

  fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(
      lookup: ResourceLookupType
  ): RuntimeType?

  fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> ensureLookup(
      lookup: ResourceLookupType
  ): RuntimeType

  suspend fun <T> withPortForward(
      server: HetznerServerLookup,
      port: Int,
      block: suspend (Int?) -> T,
  ): T

  suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(
      clazz: KClass<*>
  ): List<RuntimeType>
}

data class ProvisionerContext(
    override val sshKeyPair: KeyPair,
    val sshKeyAbsolutePath: String,
    override val sshConfigFilePath: Path,
    override val cloudName: String,
    val environmentName: String,
    val registry: ProvisionersRegistry,
) : CloudProvisionerContext, Closeable {
  private val logger = KotlinLogging.logger {}

  val sshClients = mutableMapOf<String, SSHClient>()

  override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(
      lookup: ResourceLookupType
  ): RuntimeType? = registry.lookup(lookup, this)

  override fun <
      RuntimeType,
      ResourceLookupType : InfrastructureResourceLookup<RuntimeType>,
  > ensureLookup(lookup: ResourceLookupType): RuntimeType =
      registry.lookup(lookup, this).let {
        if (it == null) {
          logger.error { "could not find resource ${lookup.logText()}" }
        }
        it!!
      }

  override fun validateDnsZone(zone: String): Result<String> {
    val domainLookup = registry.lookup(HetznerDnsZoneLookup(zone), this)
    return if (domainLookup == null) {
      Error(
          "no zone found for root domain '$zone', please make sure that the zone can be managed by the configured cloud provider",
      )
    } else {
      Success(zone)
    }
  }

  fun createOrGetSshClient(host: String, port: Int) =
      sshClients.getOrPut("$host:$port") {
        logger.info { "creating ssh client for '$host:$port'" }
        SSHClient(host, this.sshKeyPair, port = port)
      }

  override suspend fun <T> withPortForward(
      server: HetznerServerLookup,
      port: Int,
      block: suspend (Int?) -> T,
  ): T {
    val server = this.lookup(server)

    return if (server != null) {
      createOrGetSshClient(
              server.publicIpv4 ?: throw RuntimeException("${server.logText()} has no public ip"),
              port = server.sshPort,
          )
          .portForward(port) { block.invoke(it) }
    } else {
      block.invoke(null)
    }
  }

  override suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(
      clazz: KClass<*>
  ): List<RuntimeType> = registry.list(clazz)

  override fun close() {
    sshClients.forEach {
      logger.info { "closing ssh client for '${it.key}'" }
      it.value.close()
    }

    sshClients.clear()
  }
}
