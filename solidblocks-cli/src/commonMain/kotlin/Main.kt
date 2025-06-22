import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.BlcksCommand
import de.solidblocks.cli.docs.DocsCommand
import de.solidblocks.cli.docs.ansible.AnsibleCommand
import de.solidblocks.cli.hetzner.HetznerCommand
import de.solidblocks.cli.hetzner.NukeCommand


fun main(args: Array<String>) {
    val blcks = BlcksCommand()

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

    val hetzner = HetznerCommand()
    hetzner.subcommands(NukeCommand())
    blcks.subcommands(hetzner)

    val docs = DocsCommand()
    docs.subcommands(AnsibleCommand())
    blcks.subcommands(docs)

    blcks.main(args)
}
