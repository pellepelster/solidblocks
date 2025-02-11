package de.solidblocks.hetzner.dns

import de.solidblocks.hetzner.dns.model.RecordRequest
import de.solidblocks.hetzner.dns.model.RecordType
import de.solidblocks.hetzner.dns.model.ZoneRequest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class HetznerDnsAPIIntegrationTest {

  val testZone = "integration-test-solidblocks.de"

  @Test
  fun testFlow() {
    val api = HetznerDnsApi(System.getenv("HETZNER_DNS_API_TOKEN"))

    // list all zones
    api.zones()
        .onSuccess {
          it.zones.size shouldBeGreaterThan 1
          it.meta.pagination.perPage shouldBe 100
          it.meta.pagination.page shouldBe 1
          it.meta.pagination.totalEntries shouldBe it.zones.size
          it.meta.pagination.lastPage shouldBe 1
        }
        .onFailure { fail(it) }

    // ensure deleting an invalid zone id is handled gracefully
    api.deleteZone("XXXXXXXSLaMsoTNyqrt4") shouldBe false

    // clean up from previous tests if needed
    api.zones(testZone)
        .onSuccess { it.zones.forEach { api.deleteZone(it.id) shouldBe true } }
        .onFailure { fail(it) }

    val oldZoneCount = api.zones().getOrNull()!!.zones.size

    // create zone
    api.createZone(ZoneRequest(testZone))
        .onSuccess { it.zone.name shouldBe testZone }
        .onFailure { fail(it) }

    // we should now have one zone more in total
    api.zones().getOrNull()!!.zones.size shouldBe oldZoneCount + 1

    // verify created zone
    api.zones(testZone)
        .onSuccess {
          it.zones shouldHaveSize 1
          it.zones[0].name shouldBe testZone
          it.zones[0].ttl shouldBe 86400
        }
        .onFailure { fail(it) }

    val createdZone = api.zones(testZone).getOrNull()!!.zones[0]

    // on no, the new zone has the wrong ttl, lets fix this
    api.updateZone(
        createdZone.id,
        ZoneRequest(testZone, ttl = 66),
    )

    // verify updated zone
    api.zones(testZone)
        .onSuccess {
          it.zones shouldHaveSize 1
          it.zones[0].name shouldBe testZone
          it.zones[0].ttl shouldBe 66
        }
        .onFailure { fail(it) }

    // check newly created zone has no records (not counting the SOA record)
    api.records(createdZone.id).onSuccess { it.records shouldHaveSize 1 }.onFailure { fail(it) }

    // add record to zone
    api.createRecord(
            RecordRequest(
                createdZone.id,
                RecordType.A,
                "yolo",
                "1.1.1.1",
            ),
        )
        .onSuccess {
          it.record.name shouldBe "yolo"
          it.record.value shouldBe "1.1.1.1"
        }
        .onFailure { fail(it) }

    val createdRecord =
        api.records(createdZone.id).getOrNull()!!.records.first { it.name == "yolo" }!!

    // check newly created record shows up
    api.records(createdZone.id).onSuccess { it.records shouldHaveSize 2 }.onFailure { fail(it) }

    // we made a mistake, lets try to update the record
    api.updateRecord(
            createdRecord.id,
            RecordRequest(
                createdZone.id,
                RecordType.A,
                "yolo1",
                "1.1.1.2",
            ),
        )
        .onSuccess {
          it.record.name shouldBe "yolo1"
          it.record.value shouldBe "1.1.1.2"
        }
        .onFailure { fail(it) }

    // clean up test zone
    api.zones(testZone)
        .onSuccess { it.zones.forEach { api.deleteZone(it.id) shouldBe true } }
        .onFailure { fail(it) }
  }
}
