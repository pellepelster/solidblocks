package de.solidblocks.cli.commands

import de.solidblocks.cli.commands.environment.agents.RunningInstance
import de.solidblocks.cloud.model.ModelConstants
import de.solidblocks.cloud.model.entities.Role
import me.tomsdevsn.hetznercloud.HetznerCloudAPI

class InstanceManager(private val cloudApi: HetznerCloudAPI) {

    fun allServers() = cloudApi.servers.servers.map {
        RunningInstance(it.name, it.publicNet.ipv4.ip, Role.valueOf(it.labels[ModelConstants.ROLE_LABEL]!!))
    }
}
