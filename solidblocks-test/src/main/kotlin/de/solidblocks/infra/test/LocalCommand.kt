import de.solidblocks.infra.test.log
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.comparables.gt
import io.kotest.matchers.comparables.lt
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.absolutePathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

fun haveExitCode(exitCode: Int) = Matcher<CommandRunResult> { value ->
    MatcherResult(
        value.result.exitCode == exitCode,
        { "exit code was ${value.result.exitCode} but we expected $exitCode" },
        { "exit code should not be $exitCode" },
    )
}

fun unmatchedWaitForLogLinesEqual(size: Int) = Matcher<CommandRunResult> { value ->
    MatcherResult(
        value.unmatchedWaitForLogLines.size == size,
        {
            "all wait for log lines conditions should have matched, but the following expressions were not matched ${
                value.unmatchedWaitForLogLines.joinToString(", ") { "'${it.regex}'" }
            }"
        },
        { "all wait for log lines expressions have matched, but at least $size expressions should not have matched" },
    )
}


infix fun CommandRunResult.shouldHaveExitCode(exitCode: Int): CommandRunResult {
    this should haveExitCode(exitCode)
    return this
}

infix fun CommandRunResult.runtimeShouldBeLessThan(duration: Duration): CommandRunResult {
    this.result.runtime shouldBe lt(duration)
    return this
}

fun CommandRunResult.shouldNotHaveUnmatchedWaitForLogLines(): CommandRunResult {
    this should unmatchedWaitForLogLinesEqual(0)
    return this
}

infix fun CommandRunResult.shouldHaveUnmatchedWaitForLogLines(expected: Int): CommandRunResult {
    this should unmatchedWaitForLogLinesEqual(expected)
    return this
}

infix fun CommandRunResult.runtimeShouldBeGreaterThan(duration: Duration): CommandRunResult {
    this.result.runtime shouldBe gt(duration)
    return this
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val runtime: Duration
)


data class CommandRunResult(
    val unmatchedWaitForLogLines: Queue<WaitForLogline> = LinkedList(),
    val result: CommandResult
)

data class CommandRun(
    val start: TimeSource.Monotonic.ValueTimeMark,
    val result: Deferred<CommandResult>,
)

data class WaitForLogline(val regex: Regex, val timeout: Duration)

class LocalCommandBuilder(private val executable: String) {

    private var timeout: Duration = Int.MAX_VALUE.seconds

    private val waitForLogLines: Queue<WaitForLogline> = LinkedList()

    private val unmatchedWaitForLogLines: Queue<WaitForLogline> = LinkedList()

    fun timeout(timeout: Duration) = apply { this.timeout = timeout }

    fun waitForLogLine(regex: Regex, timeout: Duration = 5.seconds) = apply {
        this.waitForLogLines.add(WaitForLogline(regex, timeout))
    }

    fun run(): CommandRunResult =
        runBlocking {

            val stream: Queue<StreamEntry> = LinkedList()

            val commandRunContext = this.runCommand(Path.of(executable), timeout = timeout, onStream = {
                stream.add(it)
            })

            println("YYYY")
            while (waitForLogLines.isNotEmpty()) {
                println("ZZZZ")
                val waitFor = waitForLogLines.remove()

                log(TimeSource.Monotonic.markNow() - commandRunContext.start, "waiting for log line '${waitFor}'")

                try {
                    withTimeout(waitFor.timeout) {
                        var matched = false
                        while (!matched) {
                            if (!stream.isEmpty()) {
                                val entry = stream.remove()
                                matched = entry.line.matches(waitFor.regex)
                            }
                            sleep(10)
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    log(
                        TimeSource.Monotonic.markNow() - commandRunContext.start,
                        "timeout exceeded waiting for log line '${waitFor.regex}' (${waitFor.timeout})"
                    )
                    unmatchedWaitForLogLines.add(waitFor)
                }
            }

            CommandRunResult(unmatchedWaitForLogLines, commandRunContext.result.await())
        }
}

class LocalCommandRunningAssertionBuilder() {
}

class LocalCommandResultAssertionBuilder() {
}

fun localCommand(executable: Path) = localCommand(executable.absolutePathString())

fun localCommand(executable: String) = LocalCommandBuilder(executable)

enum class StreamType { stdout, stderr }

data class StreamEntry(val timestamp: Duration, val line: String, val type: StreamType)

suspend fun CoroutineScope.runCommand(
    command: Path,
    timeout: Duration,
    args: List<String> = emptyList(),
    onStream: (entry: StreamEntry) -> Unit = {},
): CommandRun {

    val cmdArgs = listOf(command.toAbsolutePath().toString()) + args
    val stream = mutableListOf<StreamEntry>()
    val process = ProcessBuilder(cmdArgs).start()
    val start = TimeSource.Monotonic.markNow()
    log(start - start, "started command '${command}'")

    val result = async {
        try {
            val stdoutHandle = thread {
                process.inputStream.bufferedReader(Charset.defaultCharset()).forEachLine {
                    val timestamp = TimeSource.Monotonic.markNow() - start
                    log(timestamp, it)
                    val entry = StreamEntry(timestamp, it, StreamType.stdout)
                    stream.add(entry)
                    onStream.invoke(entry)
                }
            }

            val stderrHandle = thread {
                process.errorStream.bufferedReader(Charset.defaultCharset()).forEachLine {
                    val timestamp = TimeSource.Monotonic.markNow() - start
                    log(timestamp, it)
                    val entry = StreamEntry(timestamp, it, StreamType.stderr)
                    stream.add(entry)
                    onStream.invoke(entry)
                }
            }

            process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)
            val end = TimeSource.Monotonic.markNow()

            killProcessAndWait(process)

            stdoutHandle.interrupt()
            stderrHandle.interrupt()

            CommandResult(
                exitCode = process.exitValue(),
                stdout = stream.filter { it.type == StreamType.stdout }.joinToString("\n") { it.line },
                stderr = stream.filter { it.type == StreamType.stderr }.joinToString("\n") { it.line },
                runtime = end.minus(start)
            )
        } finally {
            killProcessAndWait(process)
        }
    }

    return CommandRun(start, result)
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