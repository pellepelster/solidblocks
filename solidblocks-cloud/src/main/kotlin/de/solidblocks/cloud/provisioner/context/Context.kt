package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.utils.Result
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

    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<out InfrastructureResourceLookup<*>>): List<RuntimeType>

    suspend fun destroy(lookup: InfrastructureResourceLookup<*>, log: LogContext): Boolean

    fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R>
}

interface ValidationContext {
    suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<out InfrastructureResourceLookup<*>>): List<RuntimeType>
}

interface ProvisionerApplyContext : SSHProvisionerContext {
    suspend fun createSecret(path: String, secret: String): Result<Unit>
}

interface ProvisionerDiffContext : SSHProvisionerContext {
    fun hasPendingChange(resource: BaseResource): Boolean
}