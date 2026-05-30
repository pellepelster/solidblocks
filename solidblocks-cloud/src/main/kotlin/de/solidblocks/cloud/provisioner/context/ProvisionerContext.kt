package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.Constants.sshHostPrivateKeySecretPath
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.secret.GenericSecretLookup
import de.solidblocks.cloud.services.*
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.utils.io.core.*
import java.security.KeyPair
import java.security.PublicKey
import kotlin.collections.plus
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class ValidationContextImpl(
    environment: EnvironmentContext,
    val registry: ProvisionersRegistry,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
) : ValidationContext {
    override suspend fun <LookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<out InfrastructureResourceLookup<*>>): List<LookupType> =
        registry.list(clazz)
}

open class ProvisionerContextImpl(
    override val sshKeyPair: KeyPair,
    override val sshKeyAbsolutePath: String,
    environment: EnvironmentContext,
    registry: ProvisionersRegistry,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
) : BaseSSHProvisionerContextImpl(environment, registry, serviceRegistrations), SSHProvisionerContext, Closeable {
    val sshClients = mutableMapOf<String, SSHClient>()

    private fun getOpenSshHostPublicKey(serverName: String): PublicKey? {
        val secretPath = sshHostPrivateKeySecretPath(environment, serverName, KeyType.ed25519)
        logger.info { "loading ssh host key from '$secretPath'" }
        val secret = lookup(GenericSecretLookup(secretPath))
        return secret?.secret?.let { SSHKeyUtils.loadKey(it).public }
    }

    override fun createOrGetSshClient(serverName: String): Result<SSHClient> {
        val server = this.lookup(HetznerServerLookup(serverName)) ?: return Error<SSHClient>("failed to create ssh client for '$serverName'")
        val publicIpv4 = server.publicIpv4 ?: throw RuntimeException("${server.logText()} has no public ip")
        val publicKey = getOpenSshHostPublicKey(server.name) ?: throw RuntimeException("no host key found for ${server.logText()}")

        logger.info { "creating ssh client for '${server.name}:${server.sshPort}'" }
        val sshClient = try {
            SSHClient(publicIpv4, this.sshKeyPair, publicKey, port = server.sshPort)
        } catch (e: Exception) {
            return Error(e.message ?: "failed to create ssh client")
        }

        return sshClients.getOrPut("${server.name}:${server.sshPort}") {
            sshClient
        }.let { Success(it) }
    }

    override fun close() {
        sshClients.forEach {
            logger.info { "closing ssh client for '${it.key}'" }
            it.value.close()
        }

        sshClients.clear()
    }

    override fun interpolationRegistry() = registry.interpolationRegistry
}

abstract class BaseSSHProvisionerContextImpl(
    override val environment: EnvironmentContext,
    val registry: ProvisionersRegistry,
    val serviceRegistrations: List<ServiceRegistration<*, *>>,
) : SSHProvisionerContext {

    override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType? = registry.lookup(lookup, this)

    override suspend fun <LookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<out InfrastructureResourceLookup<*>>): List<LookupType> =
        registry.list(clazz)

    override suspend fun destroy(lookup: InfrastructureResourceLookup<*>, log: LogContext) = registry.destroy(lookup, this, log)

    override fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R> = serviceRegistrations.managerForService(runtime)
}

fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> ProvisionerContext.ensureLookup(lookup: ResourceLookupType): RuntimeType = this.lookup(lookup).let {
    it ?: throw RuntimeException("could not find resource ${lookup.logText()}")
}

fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> ProvisionerContext.ensureOptionalLookup(lookup: ResourceLookupType?): RuntimeType? = if (lookup == null) {
    return null
} else {
    this.ensureLookup(lookup)
}
