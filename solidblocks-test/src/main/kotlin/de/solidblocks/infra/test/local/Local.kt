import de.solidblocks.infra.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.time.TimeSource

class LocalCommandBuilder(executable: String) : CommandBuilder(executable) {

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun runInternal(
        start: TimeSource.Monotonic.ValueTimeMark,
        stdin: Channel<String>,
        output: (entry: OutputLine) -> Unit
    ): Deferred<ProcessResult> =
        withContext(Dispatchers.IO) {
            async {
                val cmdArgs = listOf(executable)
                val process = ProcessBuilder(cmdArgs).start()
                val stdinWriter = process.outputWriter()

                log(start - start, "started command '${executable}'")

                launch {
                    while (process.isAlive && !stdin.isClosedForReceive) {
                        yield()
                        val result = stdin.tryReceive()
                        if (result.isSuccess) {
                            result.getOrNull()?.let {
                                stdinWriter.write(it)
                                stdinWriter.newLine()
                                stdinWriter.flush()
                            }
                        }
                    }
                }

                try {
                    launch {
                        process.inputStream.bufferedReader(Charset.defaultCharset()).lineSequence().asFlow().flowOn(
                            Dispatchers.IO
                        )
                            .collect {
                                val timestamp = TimeSource.Monotonic.markNow() - start
                                val entry = OutputLine(timestamp, it, OutputType.stdout)
                                output.invoke(entry)
                            }
                    }

                    launch {
                        process.errorStream.bufferedReader(Charset.defaultCharset()).lineSequence().asFlow().flowOn(
                            Dispatchers.IO
                        )
                            .collect {
                                val timestamp = TimeSource.Monotonic.markNow() - start
                                val entry = OutputLine(timestamp, it, OutputType.stderr)
                                output.invoke(entry)
                            }
                    }

                    if (!process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
                        log(start, "timeout for command exceeded (${timeout})")
                    }

                    /*
                    if (!stdin.isClosedForSend) {
                        stdin.cancel()
                    }
                    */

                    val end = TimeSource.Monotonic.markNow()
                    killProcessAndWait(process)

                    ProcessResult(
                        exitCode = process.exitValue(),
                        runtime = end.minus(start)
                    )
                } finally {
                    killProcessAndWait(process)
                }
            }

        }

    private fun killProcessAndWait(process: Process) {
        sleep(100)

        Runtime.getRuntime().exec("kill -SIGINT ${process.pid()}");
        process.descendants().forEach { it.destroyForcibly() }

        while (process.isAlive) {
            sleep(10)
        }
    }
}

class LocalTestContext : TestContext<LocalCommandBuilder> {
    override fun command(executable: Path) = LocalCommandBuilder(executable.absolutePathString())

    override fun toString(): String {
        return "LocalTestContext()"
    }
}


fun local() = LocalTestContext()


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