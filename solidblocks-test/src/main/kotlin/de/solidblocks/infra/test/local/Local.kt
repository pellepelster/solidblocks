import de.solidblocks.infra.test.*
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.TimeSource

class LocalCommandBuilder(executable: String) : CommandBuilder(executable) {

    override fun runInternal(): CommandRunResult =
        runBlocking {
            withContext(Dispatchers.IO) {
                val start = TimeSource.Monotonic.markNow()
                val output: Queue<OutputLine> = LinkedList()

                val command = async {
                    runCommand(executable, start = start, timeout = timeout, onOutput = {
                        output.add(it)
                    })
                }

                val waitForMatchers = async {
                    waitForOutput1(start, waitForOutput, output)
                }

                joinAll(command, waitForMatchers)
                CommandRunResult(waitForMatchers.await(), command.await())
            }
        }
}

class LocalTestContext : TestContext {
    override fun command(executable: Path) = LocalCommandBuilder(executable.absolutePathString())

    override fun toString(): String {
        return "LocalTestContext()"
    }
}

fun local(): TestContext = LocalTestContext()

fun runCommand(
    command: String,
    timeout: Duration,
    start: TimeSource.Monotonic.ValueTimeMark,
    args: List<String> = emptyList(),
    onOutput: (entry: OutputLine) -> Unit = {},
): CommandResult {

    val cmdArgs = listOf(command) + args
    val output = mutableListOf<OutputLine>()
    val process = ProcessBuilder(cmdArgs).start()
    log(start - start, "started command '${command}'")

    return try {
        val stdoutHandle = thread {
            process.inputStream.bufferedReader(Charset.defaultCharset()).forEachLine {
                val timestamp = TimeSource.Monotonic.markNow() - start
                log(timestamp, it)
                val entry = OutputLine(timestamp, it, OutputType.stdout)
                output.add(entry)
                onOutput.invoke(entry)
            }
        }

        val stderrHandle = thread {
            process.errorStream.bufferedReader(Charset.defaultCharset()).forEachLine {
                val timestamp = TimeSource.Monotonic.markNow() - start
                log(timestamp, it)
                val entry = OutputLine(timestamp, it, OutputType.stderr)
                output.add(entry)
                onOutput.invoke(entry)
            }
        }

        if (!process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
            log(start, "timeout for command exceeded (${timeout})")
        }
        val end = TimeSource.Monotonic.markNow()

        killProcessAndWait(process)

        stdoutHandle.interrupt()
        stderrHandle.interrupt()

        CommandResult(
            exitCode = process.exitValue(),
            internalOutput = output,
            runtime = end.minus(start)
        )
    } finally {
        killProcessAndWait(process)
    }
}

private fun killProcessAndWait(process: Process) {
    process.descendants().forEach { it.destroyForcibly() }
    while (process.isAlive) {
        sleep(10)
    }
}


/*
suspend fun executeShellCommand(
    command: String,
    vararg args: String
): CommandResult =
    executeCommand(
        Path.of("/bin/sh"),
        "-c",
        (listOf(command) + args)
            .joinToString(" ")
    )
*/