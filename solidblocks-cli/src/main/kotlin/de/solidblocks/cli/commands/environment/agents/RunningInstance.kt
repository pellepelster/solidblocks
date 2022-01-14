package de.solidblocks.cli.commands.environment.agents

import de.solidblocks.cloud.model.entities.NodeRole

data class RunningInstance(val name: String, val publicIp: String, val nodeRole: NodeRole)
