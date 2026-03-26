package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.provisioner.Provisioner
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeProvisioner
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.ssh.SSHKeyUtils
import de.solidblocks.utils.LogContext
import java.nio.file.Path

class TestContextUtils

val TEST_CLOUD_CONFIGURATION =
    CloudConfiguration("testCloudName", "cloud1.test-blcks.de", emptyList(), emptyList())

val TEST_PROVISIONER_CONTEXT =
    ProvisionerContext(
        SSHKeyUtils.loadKey(
            TestContextUtils::class.java.getResource("/test_ed25519.key").readText(),
        ),
        TestContextUtils::class.java.getResource("/test_ed25519.key").path,
        Path.of("."),
        "testCloudName",
        "testEnvironment",
        ProvisionersRegistry(),
    )

data class HetznerTestContext(
    val provisioner: Provisioner,
    val serverProvisioner: HetznerServerProvisioner,
    val context: ProvisionerContext,
) {

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
          TEST_PROVISIONER_CONTEXT.copy(
              registry = registry,
          ),
      )
    }
  }
}

val TEST_LOG_CONTEXT = LogContext()

val TEST_KEYWORD_HELP = KeywordHelp("TODO")
