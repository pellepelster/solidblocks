package de.solidblocks.cli.workflow.commands

import com.github.ajalt.clikt.core.CliktCommand
import de.solidblocks.cli.utils.LogType.solidblocks
import de.solidblocks.cli.utils.logError
import de.solidblocks.cli.utils.logInfo
import de.solidblocks.cli.utils.logSuccess
import de.solidblocks.cli.workflow.Workflow

class WorkflowCheckCommand(private val workflow: Workflow) : CliktCommand(
    name = "check",
    help = "Verify that all preconditions for running the workflow are met",
) {
    override fun run() {
        logInfo(solidblocks, "checking workflow preconditions")

        val success = workflow.requirements.map {
            val result = it.check()
            if (result.success) {
                logSuccess(solidblocks, result.message)
            } else {
                logError(solidblocks, result.message)
            }

            result.success
        }.all { it }


        if (success) {
            logSuccess(solidblocks, "workflow precondition checks successful")
        } else {
            logError(solidblocks, "workflow precondition checks failed")
        }
    }
}
