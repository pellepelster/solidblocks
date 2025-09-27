package de.solidblocks.hetzner.dns

import de.solidblocks.hetzner.dns.model.RecordRequest
import de.solidblocks.hetzner.dns.model.RecordType
import de.solidblocks.hetzner.dns.model.ZoneRequest
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class HetznerDnsAPIIntegrationTest {

    val testZone = "integration-test-solidblocks.de"

    val api = HetznerDnsApi(System.getenv("HETZNER_DNS_API_TOKEN"))

    @Test
    fun testFlow() {
        runBlocking {

            // list all zones
            assertSoftly(api.zones()!!) {
                it.zones.size shouldBeGreaterThan 1
                it.meta.pagination.perPage shouldBe 100
                it.meta.pagination.page shouldBe 1
                it.meta.pagination.totalEntries shouldBe it.zones.size
                it.meta.pagination.lastPage shouldBe 1
            }

            // ensure deleting an invalid zone id is handled gracefully
            api.deleteZone("XXXXXXXSLaMsoTNyqrt4") shouldBe false

            // clean up from previous tests if needed
            assertSoftly(api.zones(testZone)) {
                it?.zones?.forEach { api.deleteZone(it.id) shouldBe true }
            }

            val oldZoneCount = api.zones()!!.zones.size

            // create zone
            assertSoftly(api.createZone(ZoneRequest(testZone))!!) { it.zone.name shouldBe testZone }

            // we should now have one zone more in total
            api.zones()!!.zones.size shouldBe oldZoneCount + 1

            // verify created zone
            assertSoftly(api.zones(testZone)!!) {
                it.zones shouldHaveSize 1
                it.zones[0].name shouldBe testZone
                it.zones[0].ttl shouldBe 86400
            }

            val createdZone = api.zones(testZone)!!.zones[0]

            // on no, the new zone has the wrong ttl, lets fix this
            api.updateZone(
                createdZone.id,
                ZoneRequest(testZone, ttl = 66),
            )

            // verify updated zone
            assertSoftly(api.zones(testZone)!!) {
                it.zones shouldHaveSize 1
                it.zones[0].name shouldBe testZone
                it.zones[0].ttl shouldBe 66
            }

            // check newly created zone has no records (not counting the SOA record)
            api.records(createdZone.id)!!.records shouldHaveSize 1

            // add record to zone
            assertSoftly(
                api.createRecord(
                    RecordRequest(
                        createdZone.id,
                        RecordType.A,
                        "yolo",
                        "1.1.1.1",
                    ),
                )!!
            ) {
                it.record.name shouldBe "yolo"
                it.record.value shouldBe "1.1.1.1"
            }

            val createdRecord =
                api.records(createdZone.id)!!.records.first { it.name == "yolo" }

            // check newly created record shows up
            api.records(createdZone.id)!!.records shouldHaveSize 2

            // we made a mistake, lets try to update the record
            assertSoftly(
                api.updateRecord(
                    createdRecord.id,
                    RecordRequest(
                        createdZone.id,
                        RecordType.A,
                        "yolo1",
                        "1.1.1.2",
                    ),
                )!!
            ) {
                it.record.name shouldBe "yolo1"
                it.record.value shouldBe "1.1.1.2"
            }

            // clean up test zone
            api.zones(testZone)!!.zones.forEach { api.deleteZone(it.id) shouldBe true }
        }
    }
}
