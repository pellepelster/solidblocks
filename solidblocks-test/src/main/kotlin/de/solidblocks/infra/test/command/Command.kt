package de.solidblocks.infra.test.command

import de.solidblocks.infra.test.LogType
import de.solidblocks.infra.test.log
import de.solidblocks.infra.test.output.OutputLine
import de.solidblocks.infra.test.output.OutputType
import de.solidblocks.infra.test.output.TimestampedOutputLine
import java.io.Closeable
import java.nio.file.Path
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

data class CommandResult<O : OutputLine>(
    val exitCode: Int,
    val runtime: Duration,
    private val internalOutput: List<O>,
) {
  val stdout: String
    get() =
        internalOutput.filter { it.type == OutputType.STDOUT }.joinToString("") { "${it.line}\n" }

  val stderr: String
    get() =
        internalOutput.filter { it.type == OutputType.STDERR }.joinToString("") { "${it.line}\n" }

  val output: String
    get() = internalOutput.joinToString("") { "${it.line}\n" }
}

data class ProcessResult(
    val exitCode: Int,
    val runtime: Duration,
)

abstract class CommandBuilder(protected var command: Array<String>) : Closeable {

  protected var timeout: Duration = 60.seconds

  protected var workingDir: Path? = null

  protected val assertions: Queue<(CommandRunAssertion) -> Unit> = LinkedList()

  private var defaultWaitForOutput: Duration = 60.seconds

  private var envs = mutableMapOf<String, String>()

  private var inheritEnv = true

  fun env(env: Pair<String, String>) = apply { this.envs[env.first] = env.second }

  fun inheritEnv(inheritEnv: Boolean) = apply { this.inheritEnv = inheritEnv }

  fun env(envs: Map<String, String>) = apply { this.envs.putAll(envs) }

  fun defaultWaitForOutput(defaultWaitForOutput: Duration) = apply {
    this.defaultWaitForOutput = defaultWaitForOutput
  }

  fun workingDir(workingDir: Path) = apply { this.workingDir = workingDir }

  fun timeout(timeout: Duration) = apply { this.timeout = timeout }

  fun runResult() = runBlocking { run().result() }

  suspend fun run() =
      withContext(Dispatchers.IO) {
        val output = Collections.synchronizedList(mutableListOf<TimestampedOutputLine>())
        val stdin = Channel<String>()
        val start = TimeSource.Monotonic.markNow()

        val commandRunner = createCommandRunner(start)

        val assertionsResult = async {
          assertions.map {
            it.invoke(
                CommandRunAssertion(start, commandRunner, stdin, output, defaultWaitForOutput),
            )
          }
        }

        val result =
            commandRunner.runCommand(command, envs, inheritEnv, stdin) {
              output.add(it)
              log(
                  start,
                  it.line,
                  when (it.type) {
                    OutputType.STDOUT -> LogType.STDOUT
                    OutputType.STDERR -> LogType.STDERR
                  },
              )
            }

        CommandRun(
            start,
            commandRunner,
            stdin,
            result,
            output,
            assertionsResult,
            defaultWaitForOutput,
        )
      }

  abstract suspend fun createCommandRunner(
      start: TimeSource.Monotonic.ValueTimeMark,
  ): CommandRunner

  fun assert(assertion: (CommandRunAssertion) -> Unit) = apply { this.assertions.add(assertion) }
}
