package de.solidblocks.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.kgit2.kommand.process.Command
import com.kgit2.kommand.process.Stdio
import de.solidblocks.cli.workflow.Task

class TaskCommand(private val task: Task) : CliktCommand(name = task.name, help = task.help ?: "<no help>") {
    override fun run() {
        val child = Command("ping")
            .args(listOf("-c", "5", "localhost"))
            .stdout(Stdio.Pipe)
            .spawn()
        child.bufferedStdout()?.lines()?.forEach { line ->
            println(line)
        }
        child.wait()
    }
}