package de.solidblocks.hetzner.cloud

import de.solidblocks.hetzner.cloud.resources.DnsRRSetRecord
import de.solidblocks.hetzner.cloud.resources.DnsRRSetsCreateRequest
import de.solidblocks.hetzner.cloud.resources.DnsRRSetsRecordsUpdateRequest
import de.solidblocks.hetzner.cloud.resources.DnsRRSetsTTLUpdateRequest
import de.solidblocks.hetzner.cloud.resources.RRType
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DnsApiTest : BaseTest() {

    @Test
    fun testDnsZones() {
        runBlocking {
            val zones = api.dnsZones.list()
            zones shouldHaveAtLeastSize 0

            zones.forEach { zone ->
                zone.id shouldNotBe null
                zone.name shouldNotBe null
            }
        }
    }

    @Test
    fun testListDnsRrSets() {
        runBlocking {
            val zones = api.dnsZones.list()
            if (zones.isEmpty()) return@runBlocking

            val zone = zones.first()
            val rrSets = api.dnsRrSets(zone.id.toString()).list()
            rrSets shouldHaveAtLeastSize 0
        }
    }

    @Test
    fun testDnsRRSetFlow() {
        runBlocking {
            val zones = api.dnsZones.list()
            if (zones.isEmpty()) return@runBlocking

            val zone = zones.first()
            val rrSetsApi = api.dnsRrSets(zone.id.toString())

            val created = rrSetsApi.create(
                DnsRRSetsCreateRequest(
                    name = "test-record",
                    type = RRType.TXT,
                    ttl = 300,
                    records = listOf(DnsRRSetRecord("\"test-value\"")),
                ),
            )
            created.rrset shouldNotBe null
            created.rrset.name shouldBe "test-record"
            created.rrset.type shouldBe RRType.TXT

            rrSetsApi.waitForAction(created.action)

            val fetched = rrSetsApi.get("test-record", RRType.TXT)
            fetched shouldNotBe null
            fetched!!.rrset.records shouldHaveAtLeastSize 1

            val updateRecordsAction = rrSetsApi.updateRecords(
                "test-record",
                RRType.TXT,
                DnsRRSetsRecordsUpdateRequest(listOf(DnsRRSetRecord("\"updated-value\""))),
            )
            updateRecordsAction shouldNotBe null
            rrSetsApi.waitForAction(updateRecordsAction)

            val updateTtlAction = rrSetsApi.updateTTL("test-record", RRType.TXT, DnsRRSetsTTLUpdateRequest(600))
            updateTtlAction shouldNotBe null
            rrSetsApi.waitForAction(updateTtlAction)

            rrSetsApi.delete("test-record", RRType.TXT) shouldBe true
        }
    }
}
