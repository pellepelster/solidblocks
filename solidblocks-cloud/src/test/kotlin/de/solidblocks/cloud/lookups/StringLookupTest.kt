package de.solidblocks.cloud.lookups

import de.solidblocks.cloud.TestProvisionerContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZone
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneProvisioner
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
class StringLookupTest {

  @Test
  @Disabled("Not implemented yet")
  fun testFlow(context: SolidblocksTestContext) {
    runBlocking {
      val zoneProvisioner = HetznerDnsZoneProvisioner(System.getenv("HCLOUD_TOKEN"))

      val context =
          TestProvisionerContext(
              ProvisionersRegistry(
                  listOf(zoneProvisioner),
              ),
          )

      val zone = HetznerDnsZone("blcks-test.de")
      assertSoftly(zoneProvisioner.lookup(zone.asLookup(), context)!!) {
        it.name shouldBe "blcks-test.de"
      }
      zoneProvisioner.lookup(HetznerDnsZone("invalid").asLookup(), context) shouldBe null

      context.lookup(
          StringLookup { it.ensureLookup(zone.asLookup()).name },
      ) shouldBe "sd"
    }
  }
}
