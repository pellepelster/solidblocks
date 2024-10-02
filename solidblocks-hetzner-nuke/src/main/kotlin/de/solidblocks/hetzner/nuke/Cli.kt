package de.solidblocks.hetzner.nuke

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.util.regex.Pattern

class CliCommand : CliktCommand() {
  override fun run() {}
}

public fun validLabelParameter(label: String): Boolean {
  val labelParts = label.split("=")

  if (labelParts.size != 2) {
    return false
  }

  val regex = "^[a-z0-9A-Z][a-z0-9A-Z\\.\\-_]*[a-z0-9A-Z]$"

  val pattern = Pattern.compile(regex, Pattern.MULTILINE)
  val matcher = pattern.matcher(labelParts[0])

  if (!matcher.find()) {
    return false
  }

  return true
}

class NukeCommand : CliktCommand(help = "delete all Hetzner cloud resources") {
  private val hcloudToken by option("--hcloud-token", envvar = "HCLOUD_TOKEN").required()

  /*
  private val labels: List<String> by option("--label").multiple().validate {
  }
   */

  override fun run() {
    Nuker(hcloudToken).deleteAll(true)
  }
}

class SimulateCommand : CliktCommand(help = "simulate deletion of all Hetzner cloud resources") {
  private val hcloudToken by option("--hcloud-token", envvar = "HCLOUD_TOKEN").required()

  override fun run() {
    Nuker(hcloudToken).deleteAll(false)
  }
}

fun main(args: Array<String>) =
    CliCommand().subcommands(SimulateCommand(), NukeCommand()).main(args)
