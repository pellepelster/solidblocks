package de.solidblocks.cloud

import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString

object Utils {

    public infix fun <T> List<T>.equalsIgnoreOrder(other: List<T>) = this.size == other.size && this.toSet() == other.toSet()

    fun nextNetwork(network: String, prefixBits: Int, exclude: Set<String> = emptySet()) =
        listAllNetworks(network, prefixBits).subtract(exclude).subtract(setOf(network)).firstOrNull() ?: throw RuntimeException("network not found")

    private fun listAllNetworks(network: String, prefixBits: Int): List<String> {
        val subnet: IPAddress = IPAddressString(network).address
        val newSubnets: IPAddress = subnet.setPrefixLength(subnet.prefixLength + prefixBits, false)
        return newSubnets.prefixBlockIterator().asSequence().map { it.toString() }.toList()
    }
}

