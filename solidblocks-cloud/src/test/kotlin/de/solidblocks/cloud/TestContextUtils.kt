package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.providers.CloudConfigurationContext
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.context.ProvisionerApplyContext
import de.solidblocks.cloud.provisioner.context.ProvisionerDiffContext
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeProvisioner
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.services.ServiceConfiguration
import de.solidblocks.cloud.services.ServiceConfigurationRuntime
import de.solidblocks.cloud.services.ServiceManager
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import java.nio.file.Path
import kotlin.reflect.KClass

class TestContextUtils

class TestProvisionerContext(val registry: ProvisionersRegistry, val sshClient: SSHClient? = null) : ProvisionerApplyContext {
    override val sshKeyPair =
        SSHKeyUtils.loadKey(
            TestContextUtils::class.java.getResource("/test_ed25519.key").readText(),
        )

    override val sshConfigFilePath = Path.of(".")

    override val environment = EnvironmentContext("testCloudName", "default")

    override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType? = registry.lookup(lookup, this)

    override fun createOrGetSshClient(serverName: String) = sshClient ?: TODO("Not yet implemented")

    override suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>) = TODO("Not yet implemented")

    override fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R> = TODO("Not yet implemented")

    val secrets = mutableMapOf<String, String>()

    override suspend fun createSecret(path: String, secret: String): Result<Unit> {
        secrets[path] = secret
        return Success(Unit)
    }

    override fun hasPendingChange(resource: BaseResource) = false
}

val TEST_PROVISIONER_CONTEXT = TestProvisionerContext(ProvisionersRegistry())

val TEST_CLOUD_CONFIGURATION_CONTEXT = CloudConfigurationContext(EnvironmentContext("cloud1", "default"), Path.of("tmp"))

data class HetznerTestContext(val provisioner: Provisioner, val serverProvisioner: HetznerServerProvisioner, val context: ProvisionerApplyContext) {

    companion object {
        fun create(hcloudToken: String): HetznerTestContext {
            val sshProvisioner = HetznerSSHKeyProvisioner(hcloudToken)
            val volumeProvisioner = HetznerVolumeProvisioner(hcloudToken)
            val serverProvisioner = HetznerServerProvisioner(hcloudToken)
            val networkProvisioner = HetznerNetworkProvisioner(hcloudToken)
            val subnetProvisioner = HetznerSubnetProvisioner(hcloudToken)

            val registry =
                ProvisionersRegistry(
                    listOf(
                        UserDataLookupProvider(),
                        sshProvisioner,
                        volumeProvisioner,
                        serverProvisioner,
                        networkProvisioner,
                        subnetProvisioner,
                    ),
                    listOf(
                        sshProvisioner,
                        volumeProvisioner,
                        serverProvisioner,
                        networkProvisioner,
                        subnetProvisioner,
                    ),
                )
            val provisioner = Provisioner(registry)

            return HetznerTestContext(
                provisioner,
                serverProvisioner,
                TestProvisionerContext(registry),
            )
        }
    }
}

val TEST_LOG_CONTEXT = LogContext()

val TEST_KEYWORD_HELP = KeywordHelp("TODO")
