package de.solidblocks.provisioner.hetzner.dns.zone

data class DnsZoneRuntime(val id: String, val name: String) {

    fun id(): String {
        return id
    }
}
