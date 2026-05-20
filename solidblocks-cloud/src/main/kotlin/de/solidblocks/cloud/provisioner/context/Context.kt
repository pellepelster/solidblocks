package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.interpolation.StringInterpolationRegistry
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.catchingResult
import de.solidblocks.ssh.SSHClient
import de.solidblocks.utils.LogContext
import java.security.KeyPair
import kotlin.reflect.KClass

interface SSHProvisionerContext : ProvisionerContext {
    val sshKeyPair: KeyPair
    val sshKeyAbsolutePath: String
    fun createOrGetSshClient(serverName: String): de.solidblocks.cloud.utils.Result<SSHClient>
}

interface ProvisionerContext {
    val environment: EnvironmentContext

    fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType?

    suspend fun <LookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<out InfrastructureResourceLookup<*>>): List<LookupType>

    suspend fun destroy(lookup: InfrastructureResourceLookup<*>, log: LogContext): Boolean

    fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R>

    fun interpolationRegistry(): StringInterpolationRegistry
}

interface ValidationContext {
    suspend fun <LookupType : InfrastructureResourceLookup<RuntimeType>, RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<out InfrastructureResourceLookup<*>>): List<LookupType>
}

interface ProvisionerApplyContext : SSHProvisionerContext {
    suspend fun createSecret(path: String, secret: String, taintable: Boolean): Result<Unit>
}

interface ProvisionerDiffContext : SSHProvisionerContext {
    fun hasPendingChange(resource: BaseResource): Boolean
}

fun <T> SSHProvisionerContext.withSSHClient(serverName: String, block: (SSHClient) -> Result<T>): Result<T> {
    val sshClient = when (val result = this.createOrGetSshClient(serverName)) {
        is Error<SSHClient> -> return Error<T>(result.error)
        is Success -> result.data
    }

    return block(sshClient)
}

fun <T> SSHProvisionerContext.withCatchingSSHClient(serverName: String, block: (SSHClient) -> T) = this.withSSHClient(serverName) {
    catchingResult {
        block.invoke(it)
    }
}
