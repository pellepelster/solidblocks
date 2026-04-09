package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TestProvisionerContext
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecord
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnsrecord.HetznerDnsRecordProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZone
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.dnszone.HetznerDnsZoneRuntime
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerProvisioner
import de.solidblocks.cloud.provisioner.hetzner.cloud.server.HetznerServerRuntime
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import de.solidblocks.hetzner.cloud.resources.ServerStatus
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class HetznerDnsProvisionerTest {

    @Test
    fun testFlow(context: SolidblocksTestContext) {
        val hetzner = context.hetzner(System.getenv("HCLOUD_TOKEN"))

        val serverProvisioner = mockk<HetznerServerProvisioner>()
        val serverLookup1 = HetznerServerLookup("server1")
        val serverLookup2 = HetznerServerLookup("server2")

        coEvery { serverProvisioner.lookup(eq(serverLookup1), any()) } returns
            HetznerServerRuntime(
                1,
                "server1",
                ServerStatus.running,
                "debian12",
                HetznerServerType.cx23,
                HetznerLocation.nbg1,
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
                HetznerServerType.cx23,
                HetznerLocation.nbg1,
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

            val zones: List<HetznerDnsZoneRuntime> = zoneProvisioner.list()
            assertSoftly(zones) {
                it shouldHaveSize 1
                it[0].name shouldBe "blcks-test.de"
            }

            val context =
                TestProvisionerContext(
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
                    labels = hetzner.defaultLabels,
                )
            recordProvisioner.lookup(record.asLookup(), context) shouldBe null
            assertSoftly(recordProvisioner.diff(record, context)) {
                it.status shouldBe ResourceDiffStatus.missing
            }

            recordProvisioner.apply(record, context, TEST_LOG_CONTEXT) shouldNotBe null

            assertSoftly(recordProvisioner.lookup(record.asLookup(), context)!!) {
                it.name shouldBe recordName
                it.values shouldHaveSize 1
                it.values[0] shouldBe "127.0.0.1"
            }
            assertSoftly(recordProvisioner.diff(record, context)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            val recordNewServer =
                HetznerDnsRecord(
                    recordName,
                    HetznerDnsZone("blcks-test.de").asLookup(),
                    listOf(serverLookup2),
                    labels = hetzner.defaultLabels,
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
