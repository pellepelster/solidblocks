package de.solidblocks.cloud

import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString

object NetworkUtils {

    private val NETWORK_BIT_SHIFT = 8
    private val SOLIDBLOCKS_NETWORK = "10.0.0.0/8"

    fun solidblocksNetwork() = listAllNetworks().first()

    fun nextNetwork(exclude: Set<String> = emptySet()): String? {
        return listAllNetworks().subtract(exclude).subtract(setOf(solidblocksNetwork())).firstOrNull()
    }

    private fun listAllNetworks(): List<String> {
        val subnet: IPAddress = IPAddressString(SOLIDBLOCKS_NETWORK).address
        val newSubnets: IPAddress = subnet.setPrefixLength(subnet.prefixLength + NETWORK_BIT_SHIFT, false)
        return newSubnets.prefixBlockIterator().asSequence().map { it.toString() }.toList()
    }

    fun subnetForNetwork(network: String): String {
        val subnet: IPAddress = IPAddressString(network).address
        val newSubnets: IPAddress = subnet.setPrefixLength(subnet.prefixLength + NETWORK_BIT_SHIFT, false)
        return newSubnets.prefixBlockIterator().asSequence().map { it.toString() }.toList().first()
    }

}