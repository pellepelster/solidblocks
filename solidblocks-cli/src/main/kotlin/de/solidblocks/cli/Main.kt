package de.solidblocks.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.BlcksCommand
import de.solidblocks.cli.docs.DocsCommand
import de.solidblocks.cli.docs.ansible.AnsibleCommand
import de.solidblocks.cli.hetzner.HetznerCommand
import de.solidblocks.cli.hetzner.asg.HetznerAsgCommand
import de.solidblocks.cli.hetzner.asg.HetznerAsgRotateCommand
import de.solidblocks.cli.hetzner.nuke.HetznerNukeCommand
import de.solidblocks.cli.terraform.*

fun main(args: Array<String>) {
    val root = BlcksCommand()

    /*
    val workflowCommand = if (WorkflowParser.workflowExists()) {
        when (val result = WorkflowParser.parse(WorkflowParser.readWorkflow())) {
            is Empty -> WorkflowErrorCommand(result.message)
            is Error -> WorkflowErrorCommand(result.error)
            is Success -> {
                WorkflowCommand.createFromWorkflow(result.data)
            }
        }
    } else {
        WorkflowErrorCommand("no workflow file found at '${WorkflowParser.workflowFile}'")
    }
     */

    HetznerCommand().also {
        root.subcommands(it)
        it.subcommands(HetznerNukeCommand())
        it.subcommands(HetznerAsgCommand().subcommands(HetznerAsgRotateCommand()))
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
