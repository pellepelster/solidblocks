package de.solidblocks.cloud.provisioner.hetzner.cloud.server

import de.solidblocks.cloud.api.resources.InfrastructureResource
import de.solidblocks.cloud.api.resources.LabeledInfrastructureResource
import de.solidblocks.cloud.provisioner.hetzner.cloud.network.SubnetLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.ssh.SSHKeyLookup
import de.solidblocks.cloud.provisioner.hetzner.cloud.volume.VolumeLookup
import de.solidblocks.cloud.provisioner.userdata.UserData

class HetznerServer(
    override val name: String,
    val userData: UserData,
    val location: String = "hel1",
    val volumes: Set<VolumeLookup> = emptySet(),
    val sshKeys: Set<SSHKeyLookup> = emptySet(),
    val extraDependsOn: Set<InfrastructureResource<*, *>> = emptySet(),
    labels: Map<String, String> = emptyMap(),
    val type: String = "cx23",
    val image: String = "debian-12",
    val subnet: SubnetLookup? = null,
) : LabeledInfrastructureResource<HetznerServer, HetznerServerRuntime>(labels) {

  override val dependsOn =
      setOfNotNull(subnet, userData) + userData.dependsOn + sshKeys + extraDependsOn

  fun asLookup() = HetznerServerLookup(name)

  override fun logText() = "server '$name'"
}
