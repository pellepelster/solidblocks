package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.Constants.DEFAULT_NETWORK
import de.solidblocks.cloud.Constants.DEFAULT_SERVICE_SUBNET
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetwork
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerNetworkRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnet
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetProvisioner
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
class HetznerNetworkProvisionerTest {

  @Test
  fun testFlow(context: SolidblocksTestContext) {
    val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN"))

    val name = UUID.randomUUID().toString()
    val resource = HetznerNetwork(name, DEFAULT_NETWORK, hetzner.defaultLabels)
    val networkProvisioner = HetznerNetworkProvisioner(System.getenv("HCLOUD_TOKEN"))
    val subnetProvisioner = HetznerSubnetProvisioner(System.getenv("HCLOUD_TOKEN"))

    val registry =
        ProvisionersRegistry(
            listOf(networkProvisioner, subnetProvisioner),
            listOf(networkProvisioner, subnetProvisioner),
        )
    val context = TEST_PROVISIONER_CONTEXT.copy(registry = registry)

    runBlocking {
      val subnet = HetznerSubnet(DEFAULT_SERVICE_SUBNET, resource.asLookup())

      assertSoftly(subnetProvisioner.diff(subnet, context)!!) {
        it.status shouldBe ResourceDiffStatus.missing
      }

      // before create
      networkProvisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)?.name shouldBe null
      assertSoftly(networkProvisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.missing
        it.changes.shouldBeEmpty()
      }

      // create
      networkProvisioner
          .apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<HetznerNetworkRuntime>>()
          .data
          .name shouldBe name
      assertSoftly(networkProvisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
        it.deleteProtected shouldBe true
      }
      assertSoftly(networkProvisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.up_to_date
        it.changes.shouldBeEmpty()
      }

      // create new label
      val resourceWithNewLabel =
          HetznerNetwork(name, "10.0.0.0/8", hetzner.defaultLabels + mapOf("foo" to "bar"))
      assertSoftly(networkProvisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.has_changes
        it.changes shouldHaveSize 1
        it.changes[0].missing shouldBe true
        it.changes[0].name shouldBe "label 'foo'"
      }
      networkProvisioner
          .apply(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<HetznerNetworkRuntime>>()
          .data
          .name shouldBe name
      assertSoftly(networkProvisioner.diff(resourceWithNewLabel, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.up_to_date
        it.changes.shouldBeEmpty()
      }

      assertSoftly(networkProvisioner.diff(resource, TEST_PROVISIONER_CONTEXT)!!) {
        it.status shouldBe ResourceDiffStatus.has_changes
        it.changes shouldHaveSize 1
        it.changes[0].changed shouldBe true
        it.changes[0].name shouldBe "label 'foo'"
      }
      networkProvisioner
          .apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<HetznerNetworkRuntime>>()
          .data
          .name shouldBe name

      // update delete protection
      val resourceWithNewDeleteProtection =
          HetznerNetwork(name, "10.0.0.0/8", protected = false, labels = hetzner.defaultLabels)
      assertSoftly(
          networkProvisioner.diff(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT)!!,
      ) {
        it.status shouldBe ResourceDiffStatus.has_changes
        it.changes shouldHaveSize 1
        it.changes[0].name shouldBe "delete protection"
        it.changes[0].expectedValue shouldBe "false"
        it.changes[0].actualValue shouldBe "true"
      }

      networkProvisioner
          .apply(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
          .shouldBeTypeOf<Success<HetznerNetworkRuntime>>()
          .data
          .name shouldBe name
      assertSoftly(
          networkProvisioner.diff(resourceWithNewDeleteProtection, TEST_PROVISIONER_CONTEXT)!!,
      ) {
        it.status shouldBe ResourceDiffStatus.up_to_date
        it.changes.shouldBeEmpty()
      }

      assertSoftly(subnetProvisioner.diff(subnet, context)!!) {
        it.status shouldBe ResourceDiffStatus.missing
      }

      subnetProvisioner.apply(subnet, context, TEST_LOG_CONTEXT)

      assertSoftly(subnetProvisioner.diff(subnet, context)!!) {
        it.status shouldBe ResourceDiffStatus.up_to_date
      }
    }
  }
}
