package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServer
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.Volume
import de.solidblocks.cloud.provisioner.userdata.UserData
import de.solidblocks.cloud.provisioner.userdata.UserDataLookupProvider
import de.solidblocks.cloud.utils.Success
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HetznerServerProvisionerTest {

  @Test
  fun testFlow() {
    runBlocking {
      val sshProvisioner = HetznerSSHKeyProvisioner(System.getenv("HCLOUD_TOKEN"))
      val volumeProvisioner = HetznerVolumeProvisioner(System.getenv("HCLOUD_TOKEN"))
      val serverProvisioner = HetznerServerProvisioner(System.getenv("HCLOUD_TOKEN"))
      val name = UUID.randomUUID().toString()

      val registry =
          ProvisionersRegistry(
              listOf(
                  UserDataLookupProvider(),
                  sshProvisioner,
                  volumeProvisioner,
                  serverProvisioner,
              ),
              listOf(sshProvisioner, volumeProvisioner, serverProvisioner),
          )
      val context = TEST_PROVISIONER_CONTEXT.copy(registry = registry)
      val provisioner = Provisioner(registry)

      val sshKey1 =
          HetznerSSHKey(
              "test-key1",
              "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPb pelle@fry",
              emptyMap(),
          )
      val sshKey2 =
          HetznerSSHKey(
              "test-key2",
              "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPa pelle@fry",
              emptyMap(),
          )
      val volume =
          Volume(
              name,
              "hel1",
              16,
              protected = false,
          )

      provisioner
          .apply(listOf(sshKey1, sshKey2, volume), context, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<Unit>>()

      val server =
          HetznerServer(
              name,
              sshKeys = setOf(sshKey1.asLookup()),
              volumes = setOf(volume.asLookup()),
              userData = UserData(emptySet(), { "" }),
              location = "hel1",
          )

      runBlocking {
        serverProvisioner.lookup(server.asLookup(), context) shouldBe null
        assertSoftly(serverProvisioner.diff(server, context)!!) {
          it.status shouldBe ResourceDiffStatus.missing
        }

        serverProvisioner.apply(server, context, TEST_LOG_CONTEXT) shouldNotBe null

        assertSoftly(serverProvisioner.diff(server, context)!!) {
          it.status shouldBe ResourceDiffStatus.up_to_date
        }

        assertSoftly(
            serverProvisioner.diff(
                HetznerServer(
                    name,
                    sshKeys = setOf(sshKey1.asLookup()),
                    volumes = setOf(volume.asLookup()),
                    userData = UserData(emptySet(), { "" }),
                    location = "hel1",
                    image = "debian-99",
                ),
                context,
            )!!,
        ) {
          it.status shouldBe ResourceDiffStatus.has_changes
          it.needsRecreate() shouldBe true
          it.changes shouldHaveSize 1
          it.changes[0].name shouldBe "image"
          it.changes[0].expectedValue shouldBe "debian-99"
          it.changes[0].actualValue shouldBe "debian-12"
        }

        assertSoftly(
            serverProvisioner.diff(
                HetznerServer(
                    name,
                    sshKeys = setOf(sshKey1.asLookup(), sshKey2.asLookup()),
                    volumes = setOf(volume.asLookup()),
                    userData = UserData(emptySet(), { "" }),
                    location = "hel1",
                ),
                context,
            )!!,
        ) {
          it.status shouldBe ResourceDiffStatus.has_changes
          it.needsRecreate() shouldBe true
          it.changes shouldHaveSize 1
          it.changes[0].name shouldBe "sshKeys"
          it.changes[0].expectedValue shouldBe
              "c5383689b3a31bbd0fafbe5c62865ae53a9da27adb1f96b1d94e358f83c9bab6"
          it.changes[0].actualValue shouldBe
              "b500b5a03c1ddbd8b64d477d406ab9a11f985ad382d14fefa45541094599fcb9"
        }

        assertSoftly(serverProvisioner.lookup(server.asLookup(), context)) {
          it!!.name shouldBe name
        }

        serverProvisioner.destroy(server, context, TEST_LOG_CONTEXT)
      }
    }
  }
}
