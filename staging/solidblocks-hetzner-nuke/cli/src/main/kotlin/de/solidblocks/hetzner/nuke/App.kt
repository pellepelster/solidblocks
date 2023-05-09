package de.solidblocks.hetzner.nuke

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required


class CliCommand : CliktCommand() {
    val verbose by option().flag("--no-verbose")
    override fun run() {
        echo("Verbose mode is ${if (verbose) "on" else "off"}")
    }
}


class NukeCommand : CliktCommand() {
    val hcloudToken by option("--hcloud-token", envvar = "HCLOUD_TOKEN").required()
    override fun run() {
        val nuker = Nuker(hcloudToken)
        nuker.deleteAllVolumes(true)
    }
}

class SimulateCommand : CliktCommand() {
    val hcloudToken by option("--hcloud-token", envvar = "HCLOUD_TOKEN").required()
    override fun run() {
        val nuker = Nuker(hcloudToken)
        nuker.deleteAllVolumes(false)
    }
}

fun main(args: Array<String>) = CliCommand().subcommands(SimulateCommand(), NukeCommand()).main(args)