package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.Constants
import de.solidblocks.cloud.Constants.sshHostPrivateKeySecretPath
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.pass.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretLookup
import de.solidblocks.cloud.provisioner.pass.PassSecretRuntime
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.services.managerForService
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.core.Closeable
import java.nio.file.Path
import java.security.KeyPair
import java.security.PublicKey
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

interface CloudProvisionerContext {
    val sshKeyPair: KeyPair
    val sshConfigFilePath: Path
    val environment: EnvironmentContext

    fun validateDnsZone(zone: String): Result<String>

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType?

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> ensureLookup(lookup: ResourceLookupType): RuntimeType

    fun createOrGetSshClient(serverName: String): SSHClient

    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>): List<RuntimeType>

    fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R>

    // TODO find a more elegant solution that is also testable
    suspend fun <T> withPortForward(server: HetznerServerLookup, port: Int, block: suspend (Int?) -> T): T

    suspend fun createSecret(path: String, secret: String): Result<Unit>
}

data class ProvisionerContext(
    override val sshKeyPair: KeyPair,
    val sshKeyAbsolutePath: String,
    override val sshConfigFilePath: Path,
    override val environment: EnvironmentContext,
    val registry: ProvisionersRegistry,
    val serviceRegistrations: List<ServiceRegistration<*, *>>,
) : CloudProvisionerContext,
    Closeable {

    val sshClients = mutableMapOf<String, SSHClient>()

    override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType? = registry.lookup(lookup, this)

    override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> ensureLookup(lookup: ResourceLookupType): RuntimeType = registry.lookup(lookup, this).let {
        it ?: throw RuntimeException("could not find resource ${lookup.logText()}")
    }

    // TODO
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

    private fun getOpenSshHostPublicKey(serverName: String): PublicKey? {
        val secretPath = sshHostPrivateKeySecretPath(environment, serverName, KeyType.ed25519)
        logger.info { "loading ssh host key from '$secretPath'" }
        val secret = lookup(PassSecretLookup(secretPath))
        return secret?.secret?.let { SSHKeyUtils.loadKey(it).public }
    }

    override fun createOrGetSshClient(serverName: String): SSHClient {
        val server = this.ensureLookup(HetznerServerLookup(serverName))
        val publicIpv4 = server.publicIpv4 ?: throw RuntimeException("${server.logText()} has no public ip")
        val publicKey = getOpenSshHostPublicKey(server.name) ?: throw RuntimeException("no host key found for ${server.logText()}")

        // TODO ensure only valid sessions are cached
        return sshClients.getOrPut("${server.name}:${server.sshPort}") {
            logger.info { "creating ssh client for '${server.name}:${server.sshPort}'" }
            SSHClient(publicIpv4, this.sshKeyPair, null, port = server.sshPort)
        }
    }

    override suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>): List<RuntimeType> = registry.list(clazz)

    override fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R> = serviceRegistrations.managerForService(runtime)

    override suspend fun <T> withPortForward(server: HetznerServerLookup, port: Int, block: suspend (Int?) -> T): T = try {
        createOrGetSshClient(server.name).portForward(port) { block.invoke(it) }
    } catch (e: Exception) {
        logger.error(e) { "could not connect to server $server" }
        block.invoke(null)
    }

    override suspend fun createSecret(path: String, secret: String): Result<Unit> {
        val secret = PassSecret(
            path,
            OneTimeGeneratedSecret(secret = {
                secret
            }),
        )

        return when (val result: Result<PassSecretRuntime> = registry.apply(secret, this, LogContext())) {
            is Error<PassSecretRuntime> -> Error(result.error)
            is Success<PassSecretRuntime> -> Success(Unit)
        }
    }

    override fun close() {
        sshClients.forEach {
            logger.info { "closing ssh client for '${it.key}'" }
            it.value.close()
        }

        sshClients.clear()
    }
}
