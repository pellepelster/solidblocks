package de.solidblocks.cloud

import org.xbill.DNS.*
import org.xbill.DNS.Message.newQuery

data class DnsRecord(val resolver: String, val name: String, val values: List<String>)

class DnsService {

  val dnsServers =
      listOf(
          "208.67.222.222",
          "208.67.220.220",
          "8.8.8.8",
          "8.8.4.4",
          "1.1.1.1",
          "1.0.0.1",
          "9.9.9.9",
          "9.9.9.10",
      )

  fun tryResolveARecords(name: String): List<DnsRecord> =
      dnsServers.flatMap { resolver ->
        val records = getRecords<ARecord>(SimpleResolver(resolver), name, Type.A)
        if (records.isEmpty()) {
          listOf(DnsRecord(resolver, name, emptyList()))
        } else {
          records.map { DnsRecord(resolver, it.name.toString(), listOf(it.address.hostAddress)) }
        }
      }

  private inline fun <reified T : Record> getRecords(
      resolver: Resolver,
      name: String,
      type: Int,
  ): List<T> {
    val queryRecord = Record.newRecord(Name.fromString(name), type, DClass.IN)
    return resolver
        .send(newQuery(queryRecord))
        .getSectionRRsets(Section.ANSWER)
        .flatMap { it.rrs() }
        .filterIsInstance<T>()
  }
}
