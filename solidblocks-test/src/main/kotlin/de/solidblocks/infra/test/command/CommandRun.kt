package de.solidblocks.infra.test.command

import de.solidblocks.infra.test.output.OutputLine
import de.solidblocks.infra.test.output.OutputMatcher
import de.solidblocks.infra.test.output.waitForOutputMatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import kotlin.time.Duration
import kotlin.time.TimeSource

data class CommandRunResult(
    val result: CommandResult,
)

class CommandRunAssertion(
    private val start: TimeSource.Monotonic.ValueTimeMark,
    private val commandRunner: CommandRunner,
    private val stdin: Channel<String>,
    private val output: List<OutputLine>,
    private val defaultWaitForOutput: Duration
) {
    fun waitForOutput(regex: String, timeout: Duration = defaultWaitForOutput, answer: (() -> String)? = null) =
        runBlocking {
            waitForOutputMatcher(start, OutputMatcher(regex.toRegex(), timeout, answer), output, stdin)
        }

    fun fileExists(file: String) = runBlocking {
        val result = commandRunner.runCommand(arrayOf("test", "-f", file)) {
        }

        val processResult = result.await()
        processResult.exitCode == 0
    }

    fun sha256sum(file: String) = runBlocking {
        val output = mutableListOf<OutputLine>()
        val result = commandRunner.runCommand(arrayOf("sha256sum", file)) {
            output.add(it)
        }

        result.await()

        output.joinToString("") { it.line }.split(" ").firstOrNull()?.trim()
    }
}

interface CommandRunner : Closeable {
    suspend fun runCommand(
        command: Array<String>,
        envs: Map<String, String> = emptyMap(),
        stdin: Channel<String> = Channel(),
        output: (entry: OutputLine) -> Unit,
    ): Deferred<ProcessResult>
}

class CommandRun(
    private val start: TimeSource.Monotonic.ValueTimeMark,
    private val commandRunner: CommandRunner,
    private val stdin: Channel<String>,
    private val result: Deferred<ProcessResult>,
    private val output: List<OutputLine>,
    private val assertionsResult: Deferred<List<Unit>>,
    private val defaultWaitForOutput: Duration,
) {
    fun result() = runBlocking {
        assertionsResult.await()

        val processResult = result.await()
        commandRunner.close()

        CommandRunResult(
            CommandResult(processResult.exitCode, processResult.runtime, output),
        )
    }

    fun waitForOutput(regex: String, timeout: Duration = defaultWaitForOutput, answer: (() -> String)? = null) =
        runBlocking {
            waitForOutputMatcher(start, OutputMatcher(regex.toRegex(), timeout, answer), output, stdin)
        }

}
