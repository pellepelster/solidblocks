package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.SubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.HetznerSSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.VolumeLookup
import de.solidblocks.cloud.provisioner.userdata.UserData

class HetznerServer(
    override val name: String,
    val location: String,
    val type: String,
    val userData: UserData,
    val volumes: Set<VolumeLookup> = emptySet(),
    val sshKeys: Set<HetznerSSHKeyLookup> = emptySet(),
    val extraDependsOn: Set<InfrastructureResource<*>> = emptySet(),
    labels: Map<String, String> = emptyMap(),
    val image: String = "debian-12",
    val subnet: SubnetLookup? = null,
) : LabeledInfrastructureResource<HetznerServerRuntime>(labels) {

  override val dependsOn =
      setOfNotNull(subnet, userData) + userData.dependsOn + sshKeys + extraDependsOn

  fun asLookup() = HetznerServerLookup(name)

  override fun logText() = "server '$name'"
}
