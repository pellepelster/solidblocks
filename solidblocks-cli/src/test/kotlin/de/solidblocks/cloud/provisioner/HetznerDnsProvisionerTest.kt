package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.api.ResourceDiffStatus.missing
import de.solidblocks.cloud.api.ResourceDiffStatus.up_to_date
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecordProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZone
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import java.util.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HetznerDnsProvisionerTest {

  @Test
  fun testFlow() {
    val serverProvisioner = mockk<HetznerServerProvisioner>()
    val serverLookup1 = HetznerServerLookup("server1")
    val serverLookup2 = HetznerServerLookup("server2")

    coEvery { serverProvisioner.lookup(eq(serverLookup1), any()) } returns
        HetznerServerRuntime(
            1,
            "server1",
            ServerStatus.running,
            "debian12",
            "cx23",
            "nbg1",
            emptyMap(),
            emptyList(),
            null,
            "127.0.0.1",
            emptyList(),
        )
    coEvery { serverProvisioner.lookup(eq(serverLookup2), any()) } returns
        HetznerServerRuntime(
            1,
            "server1",
            ServerStatus.running,
            "debian12",
            "cx23",
            "nbg1",
            emptyMap(),
            emptyList(),
            null,
            "127.0.0.2",
            emptyList(),
        )
    coEvery { serverProvisioner.supportedLookupType } returns HetznerServerLookup::class

    runBlocking {
      val recordName = UUID.randomUUID().toString()

      val recordProvisioner = HetznerDnsRecordProvisioner(System.getenv("HCLOUD_TOKEN"))
      val zoneProvisioner = HetznerDnsZoneProvisioner(System.getenv("HCLOUD_TOKEN"))

      val context =
          TEST_PROVISIONER_CONTEXT.copy(
              registry =
                  ProvisionersRegistry(
                      listOf(serverProvisioner, zoneProvisioner),
                      listOf(serverProvisioner),
                  ),
          )

      val zone = HetznerDnsZone("blcks-test.de")
      assertSoftly(zoneProvisioner.lookup(zone.asLookup(), context)!!) {
        it.name shouldBe "blcks-test.de"
      }
      zoneProvisioner.lookup(HetznerDnsZone("invalid").asLookup(), context) shouldBe null

      val record =
          HetznerDnsRecord(
              recordName,
              HetznerDnsZone("blcks-test.de").asLookup(),
              listOf(serverLookup1),
          )
      recordProvisioner.lookup(record.asLookup(), context) shouldBe null
      assertSoftly(recordProvisioner.diff(record, context)) { it.status shouldBe missing }

      recordProvisioner.apply(record, context, TEST_LOG_CONTEXT) shouldNotBe null

      assertSoftly(recordProvisioner.lookup(record.asLookup(), context)!!) {
        it.name shouldBe recordName
        it.values shouldHaveSize 1
        it.values[0] shouldBe "127.0.0.1"
      }
      assertSoftly(recordProvisioner.diff(record, context)) { it.status shouldBe up_to_date }

      val recordNewServer =
          HetznerDnsRecord(
              recordName,
              HetznerDnsZone("blcks-test.de").asLookup(),
              listOf(serverLookup2),
          )

      assertSoftly(recordProvisioner.diff(recordNewServer, context)) {
        it.status shouldBe ResourceDiffStatus.has_changes
        it.changes shouldHaveSize 1
        it.changes[0].name shouldBe "value"
        it.changes[0].expectedValue shouldBe "127.0.0.2"
        it.changes[0].actualValue shouldBe "127.0.0.1"
      }
    }
  }
}
