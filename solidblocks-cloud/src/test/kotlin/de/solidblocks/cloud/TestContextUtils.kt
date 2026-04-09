package de.solidblocks.cloud

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.provisioner.CloudProvisionerContext
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
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
import de.solidblocks.ssh.SSHClient
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import java.nio.file.Path
import kotlin.reflect.KClass

class TestContextUtils

val TEST_CLOUD_CONFIGURATION =
    CloudConfiguration("testCloudName", "cloud1.test-blcks.de", emptyList(), emptyList())

class TestProvisionerContext(val registry: ProvisionersRegistry, val portMappings: Map<Int, Int> = emptyMap()) : CloudProvisionerContext {
    override val sshKeyPair =
        SSHKeyUtils.loadKey(
            TestContextUtils::class.java.getResource("/test_ed25519.key").readText(),
        )

    override val sshConfigFilePath = Path.of(".")
    override val cloudName = "testCloudName"

    override fun validateDnsZone(zone: String) = TODO("Not yet implemented")

    override fun <RuntimeType, ResourceLookupType : InfrastructureResourceLookup<RuntimeType>> lookup(lookup: ResourceLookupType): RuntimeType? = registry.lookup(lookup, this)

    override fun <
        RuntimeType,
        ResourceLookupType : InfrastructureResourceLookup<RuntimeType>,
        > ensureLookup(lookup: ResourceLookupType): RuntimeType = registry.lookup(lookup, this)!!

    override fun createOrGetSshClient(server: HetznerServerLookup): SSHClient {
        TODO("Not yet implemented")
    }

    override suspend fun <RuntimeType : BaseInfrastructureResourceRuntime> list(clazz: KClass<*>) = TODO("Not yet implemented")

    override fun <C : ServiceConfiguration, R : ServiceConfigurationRuntime> managerForService(runtime: R): ServiceManager<C, R> = TODO("Not yet implemented")
}

val TEST_PROVISIONER_CONTEXT =
    TestProvisionerContext(
        ProvisionersRegistry(),
    )

data class HetznerTestContext(val provisioner: Provisioner, val serverProvisioner: HetznerServerProvisioner, val context: CloudProvisionerContext) {

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
