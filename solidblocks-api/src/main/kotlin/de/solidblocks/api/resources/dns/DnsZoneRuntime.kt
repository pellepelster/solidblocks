package de.solidblocks.api.resources.dns

data class DnsZoneRuntime(val id: String, val name: String) {

    fun id(): String {
        return id
    }
}
