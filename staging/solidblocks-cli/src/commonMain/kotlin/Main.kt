import com.github.ajalt.clikt.core.subcommands
import de.solidblocks.cli.commands.BlcksCommand
import de.solidblocks.cli.utils.Empty
import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.workflow.WorkflowParser
import de.solidblocks.cli.workflow.commands.WorkflowCommand
import de.solidblocks.cli.workflow.commands.WorkflowErrorCommand


fun main(args: Array<String>) {

    val root = BlcksCommand()

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

    root.subcommands(workflowCommand)

    root.main(args)
}
