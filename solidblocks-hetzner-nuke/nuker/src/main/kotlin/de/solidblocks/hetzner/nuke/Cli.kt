package de.solidblocks.hetzner.nuke

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

class CliCommand : CliktCommand() {
    override fun run() {
    }
}

class NukeCommand : CliktCommand(help = "delete all Hetzner cloud resources") {
    val hcloudToken by option("--hcloud-token", envvar = "HCLOUD_TOKEN").required()
    override fun run() {
        Nuker(hcloudToken).deleteAll(true)
    }
}

class SimulateCommand : CliktCommand(help = "simulate deletion of all Hetzner cloud resources") {
    val hcloudToken by option("--hcloud-token", envvar = "HCLOUD_TOKEN").required()
    override fun run() {
        Nuker(hcloudToken).deleteAll(false)
    }
}

fun main(args: Array<String>) = CliCommand().subcommands(SimulateCommand(), NukeCommand()).main(args)