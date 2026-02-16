package de.solidblocks.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.cloud.CloudApplyCommand
import de.solidblocks.cli.cloud.CloudCommand
import de.solidblocks.cli.cloud.CloudHelpCommand
import de.solidblocks.cli.cloud.CloudPlanCommand
import de.solidblocks.cli.commands.BlcksCommand
import de.solidblocks.cli.docs.DocsCommand
import de.solidblocks.cli.docs.ansible.AnsibleCommand
import de.solidblocks.cli.github.GithubCommand
import de.solidblocks.cli.github.GithubRegistryCleanCommand
import de.solidblocks.cli.hetzner.HetznerCommand
import de.solidblocks.cli.hetzner.asg.HetznerAsgCommand
import de.solidblocks.cli.hetzner.asg.HetznerAsgRotateCommand
import de.solidblocks.cli.hetzner.nuke.HetznerNukeCommand
import de.solidblocks.cli.terraform.*

fun main(args: Array<String>) {
  val root = BlcksCommand()

  CloudCommand().also {
    root.subcommands(it)
    it.subcommands(CloudApplyCommand(), CloudPlanCommand(), CloudHelpCommand())
  }

  HetznerCommand().also {
    root.subcommands(it)
    it.subcommands(HetznerNukeCommand())
    it.subcommands(HetznerAsgCommand().subcommands(HetznerAsgRotateCommand()))
  }

  GithubCommand().also {
    root.subcommands(it)
    it.subcommands(GithubRegistryCleanCommand())
  }

  DocsCommand().also {
    root.subcommands(it)
    it.subcommands(AnsibleCommand())
  }

  TerraformCommand().also {
    root.subcommands(it)
    it.subcommands(
        BackendsCommand(TYPE.TERRAFORM).also { it.subcommands(BackendsS3Command(TYPE.TERRAFORM)) },
    )
  }

  TofuCommand().also {
    root.subcommands(it)
    it.subcommands(
        BackendsCommand(TYPE.TOFU).also { it.subcommands(BackendsS3Command(TYPE.TOFU)) },
    )
  }

  root.main(args)
}
