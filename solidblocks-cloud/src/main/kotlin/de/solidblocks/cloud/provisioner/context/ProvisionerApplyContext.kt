package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.utils.Result
import de.solidblocks.ssh.SSHClient
import kotlin.reflect.KClass

interface ProvisionerApplyContext : ProvisionerDiffContext

class ProvisionerDiffContextImpl(val pendingChanges: List<BaseInfrastructureResource<*>>, val context: ProvisionerContext) : ProvisionerApplyContext {

    override fun hasPendingChange(resource: BaseResource) = when (resource) {
        is InfrastructureResourceLookup<*> -> pendingChanges.any { pendingChanges.any { resource.isLookupFor(it) } }
        else -> pendingChanges.contains(resource)
    }

    override val sshKeyPair = context.sshKeyPair
    override val sshConfigFilePath = context.sshConfigFilePath
    override val environment = context.environment

    override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType) = context.lookup(lookup)

    override suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>): List<RuntimeType> = context.list(clazz)

    override fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R> = context.managerForService(runtime)
    override fun createOrGetSshClient(serverName: String): SSHClient = createOrGetSshClient(serverName)

    override suspend fun <T> withPortForward(server: HetznerServerLookup, port: Int, block: suspend (Int?) -> T): T = context.withPortForward(server, port, block)
    override suspend fun createSecret(path: String, secret: String): Result<Unit> = context.createSecret(path, secret)
}
