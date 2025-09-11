package de.solidblocks.cli.hetzner.api.resources

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicNet(
    @SerialName("enable_ipv4")
    val enableIpv4: Boolean,
    @SerialName("enable_ipv6")
    val enableIpv6: Boolean
)

@Serializable
data class ServerCreateRequest(
    val name: String,
    val location: String,
    @SerialName("server_type") val type: String,
    @SerialName("placement_group") val placementGroup: String? = null,
    val image: String,
    @SerialName("ssh_keys")
    val sshKeys: List<String>? = null,
    val networks: List<String>? = null,
    val firewall: List<String>? = null,
    @SerialName("user_data")
    val userData: String,
    val labels: Map<String, String>? = null,
    @SerialName("public_net")
    val publicNet: PublicNet? = null
)