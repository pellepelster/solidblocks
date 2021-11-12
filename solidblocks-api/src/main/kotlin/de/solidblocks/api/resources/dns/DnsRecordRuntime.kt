package de.solidblocks.api.resources.dns

data class DnsRecordRuntime(val id: String, val name: String, val value: String, val ttl: Int?) {

    fun id(): String {
        return id
    }
}
