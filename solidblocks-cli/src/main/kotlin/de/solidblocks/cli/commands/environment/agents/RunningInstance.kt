package de.solidblocks.cli.commands.environment.agents

import de.solidblocks.cloud.model.entities.Role

data class RunningInstance(val name: String, val publicIp: String, val role: Role)
