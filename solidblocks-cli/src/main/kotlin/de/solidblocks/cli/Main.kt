package de.solidblocks.cli

import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.BlcksCommand
import de.solidblocks.cli.docs.DocsCommand
import de.solidblocks.cli.docs.ansible.AnsibleCommand
import de.solidblocks.cli.hetzner.HetznerCommand
import de.solidblocks.cli.hetzner.NukeCommand
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
        it.subcommands(NukeCommand())
    }

    DocsCommand().also {
        root.subcommands(it)
        it.subcommands(AnsibleCommand())
    }

    TerraformCommand().also {
        root.subcommands(it)
        it.subcommands(BackendsCommand(TYPE.terraform).also {
            it.subcommands(BackendsS3Command(TYPE.terraform))
        })
    }

    TofuCommand().also {
        root.subcommands(it)
        it.subcommands(BackendsCommand(TYPE.tofu).also {
            it.subcommands(BackendsS3Command(TYPE.tofu))
        })
    }

    root.main(args)
}
