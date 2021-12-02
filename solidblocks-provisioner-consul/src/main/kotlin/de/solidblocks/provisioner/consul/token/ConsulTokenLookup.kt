package de.solidblocks.provisioner.consul.token

import java.util.*

class ConsulTokenLookup(private val id: UUID) : IConsulTokenLookup {
    override fun id() = id.toString()
}
