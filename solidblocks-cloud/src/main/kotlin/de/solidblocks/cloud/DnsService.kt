package de.solidblocks.cloud

import io.github.oshai.kotlinlogging.KotlinLogging
import org.xbill.DNS.ARecord
import org.xbill.DNS.DClass
import org.xbill.DNS.Message.newQuery
import org.xbill.DNS.Name
import org.xbill.DNS.Record
import org.xbill.DNS.Resolver
import org.xbill.DNS.Section
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type

data class DnsRecord(val resolver: String, val name: String, val values: List<String>)

class DnsService {
    private val logger = KotlinLogging.logger {}

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

    fun tryResolveARecords(name: String): List<DnsRecord> = dnsServers.flatMap { resolver ->
        val records = getRecords<ARecord>(SimpleResolver(resolver), name, Type.A)
        if (records.isEmpty()) {
            listOf(DnsRecord(resolver, name, emptyList()))
        } else {
            records.map { DnsRecord(resolver, it.name.toString(), listOf(it.address.hostAddress)) }
        }
    }

    private inline fun <reified T : Record> getRecords(resolver: Resolver, name: String, type: Int): List<T> {
        val queryRecord = Record.newRecord(Name.fromString(name), type, DClass.IN)
        return try {
            resolver
                .send(newQuery(queryRecord))
                .getSectionRRsets(Section.ANSWER)
                .flatMap { it.rrs() }
                .filterIsInstance<T>()
        } catch (e: Exception) {
            logger.error(e) {
                "failed to resolve '$name/$type'"
            }
            emptyList()
        }
    }
}
