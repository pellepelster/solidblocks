package de.solidblocks.cli.hetzner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class HetznerCommand : CliktCommand(name = "hetzner") {

    override fun help(context: Context) = "utilities around Hetzner cloud automation"

    override fun run() {
    }
}