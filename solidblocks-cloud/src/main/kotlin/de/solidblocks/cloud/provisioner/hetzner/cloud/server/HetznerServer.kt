package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseLabeledInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.HetznerSubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.HetznerVolumeLookup
import de.solidblocks.cloud.provisioner.userdata.UserData

class HetznerServer(
    name: String,
    val location: String,
    val type: String,
    val userData: UserData,
    val volumes: Set<HetznerVolumeLookup> = emptySet(),
    val sshKeys: Set<HetznerSSHKeyLookup> = emptySet(),
    dependsOn: Set<BaseInfrastructureResource<*>> = emptySet(),
    labels: Map<String, String> = emptyMap(),
    val image: String = "debian-12",
    val subnet: HetznerSubnetLookup? = null,
) : BaseLabeledInfrastructureResource<HetznerServerRuntime>(name, setOfNotNull(subnet, userData) + userData.dependsOn + sshKeys + dependsOn, labels) {

    fun asLookup() = HetznerServerLookup(name)

    override fun logText() = "server '$name'"

    override val lookupType = HetznerServerLookup::class
}
