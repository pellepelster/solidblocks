package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKey
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyRuntime
import de.solidblocks.cloud.utils.Success
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class HetznerSSHKeyProvisionerTest {

  @Test
  fun testFlow(context: SolidblocksTestContext) {
    val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN"))
    val name = UUID.randomUUID().toString()

    val resource =
        HetznerSSHKey(
            name,
            "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPb pelle@fry",
            hetzner.defaultLabels,
        )

    val provisioner = HetznerSSHKeyProvisioner(System.getenv("HCLOUD_TOKEN"))

    runBlocking {
      // before create
      provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
      assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.missing
        it.changes.shouldBeEmpty()
      }

      // create
      provisioner
          .apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<HetznerSSHKeyRuntime>>()
          .data
          .name shouldBe name
      assertSoftly(provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
        it.fingerprint shouldBe "99:fc:9b:f4:04:69:2c:9d:30:d5:2c:d9:1e:ca:b2:76"
      }
      assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.up_to_date
        it.changes.shouldBeEmpty()
      }

      // uploading the same ssh key with a different name should result in a duplicate error
      val resourceWithNewName =
          HetznerSSHKey(
              UUID.randomUUID().toString(),
              "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPb pelle@fry",
              hetzner.defaultLabels,
          )

      assertSoftly(provisioner.diff(resourceWithNewName, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.duplicate
        it.changes.shouldBeEmpty()
      }

      // add new label
      val resourceWithNewLabel =
          HetznerSSHKey(
              name,
              "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPb pelle@fry",
              hetzner.defaultLabels + mapOf("foo" to "bar"),
          )

      // diff should show missing new label
      assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.has_changes
        it.changes shouldHaveSize 1
        it.changes[0].missing shouldBe true
        it.changes[0].name shouldBe "label 'foo'"
      }
      provisioner
          .apply(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<HetznerSSHKeyRuntime>>()
          .data
          .name shouldBe name

      assertSoftly(provisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.up_to_date
        it.changes.shouldBeEmpty()
      }

      // update labels
      val resourceWithUpdatedLabel =
          HetznerSSHKey(
              name,
              "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIE+u0dEVRZDzzp4E1teCqF49r8ig3YEk8eaPqNWfDcPb pelle@fry",
              hetzner.defaultLabels + mapOf("foo" to "bar2"),
          )
      assertSoftly(provisioner.diff(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.has_changes
        it.changes shouldHaveSize 1
        it.changes[0].name shouldBe "label 'foo'"
        it.changes[0].expectedValue shouldBe "bar2"
        it.changes[0].actualValue shouldBe "bar"
      }
      provisioner
          .apply(
              resourceWithUpdatedLabel,
              TEST_PROVISIONER_CONTEXT,
              TEST_LOG_CONTEXT,
          )
          .shouldBeTypeOf<Success<HetznerSSHKeyRuntime>>()
          .data
          .name shouldBe name
      assertSoftly(provisioner.diff(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.up_to_date
        it.changes.shouldBeEmpty()
      }

      // delete
      provisioner.destroy(resourceWithUpdatedLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
      provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
    }
  }
}
