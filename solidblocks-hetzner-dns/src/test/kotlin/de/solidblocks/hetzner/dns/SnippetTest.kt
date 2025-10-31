package de.solidblocks.hetzner.dns

import de.solidblocks.hetzner.dns.model.RecordRequest
import de.solidblocks.hetzner.dns.model.RecordType
import de.solidblocks.hetzner.dns.model.ZoneRequest
import io.kotest.common.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SnippetTest {

  @Test
  @Disabled
  fun testSnippet() {
    runBlocking {
      val api = HetznerDnsApi(System.getenv("HETZNER_DNS_API_TOKEN"))

      val createdZone =
          api.createZone(ZoneRequest("my-new-zone.de"))
              ?: throw RuntimeException("Failed to create zone")
      println("created zone with id ${createdZone.zone.id}")

      val createdRecord =
          api.createRecord(RecordRequest(createdZone.zone.id, RecordType.A, "www", "192.168.0.1"))
              ?: throw RuntimeException("Failed to create zone")
      println("created record with id ${createdRecord.record.id}")
    }
  }
}
