import de.solidblocks.infra.test.CommandTestContext
import de.solidblocks.infra.test.command.CommandBuilder
import de.solidblocks.infra.test.command.CommandRunner
import de.solidblocks.infra.test.command.ProcessResult
import de.solidblocks.infra.test.local.LocalScriptBuilder
import de.solidblocks.infra.test.log
import de.solidblocks.infra.test.output.OutputLine
import de.solidblocks.infra.test.output.OutputType
import java.io.Closeable
import java.lang.Thread.sleep
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class LocalCommandBuilder(command: Array<String>) : CommandBuilder(command) {

  private val runners = mutableListOf<CommandRunner>()

  private fun killProcessAndWait(process: Process) {
    sleep(100)

    Runtime.getRuntime().exec("kill -SIGINT ${process.pid()}")
    process.descendants().forEach { it.destroyForcibly() }

    while (process.isAlive) {
      sleep(10)
    }
  }

  override suspend fun createCommandRunner(start: TimeSource.Monotonic.ValueTimeMark) =
      object : CommandRunner {
        override suspend fun runCommand(
            command: Array<String>,
            envs: Map<String, String>,
            inheritEnv: Boolean,
            stdin: Channel<String>,
            output: (entry: OutputLine) -> Unit,
        ) =
            withContext(Dispatchers.IO) {
              async {
                val processBuilder = ProcessBuilder(command.toList())

                if (!inheritEnv) {
                  processBuilder.environment().clear()
                }
                processBuilder.environment().putAll(envs)

                workingDir?.toFile().let { processBuilder.directory(it) }

                val process = processBuilder.start()
                val stdinWriter = process.outputWriter()

                log(start - start, "starting command '${command.joinToString(" ")}'")

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
                    process.inputStream
                        .bufferedReader(Charset.defaultCharset())
                        .lineSequence()
                        .asFlow()
                        .flowOn(
                            Dispatchers.IO,
                        )
                        .collect {
                          val timestamp = TimeSource.Monotonic.markNow() - start
                          val entry = OutputLine(timestamp, it, OutputType.STDOUT)
                          output.invoke(entry)
                        }
                  }

                  launch {
                    process.errorStream
                        .bufferedReader(Charset.defaultCharset())
                        .lineSequence()
                        .asFlow()
                        .flowOn(
                            Dispatchers.IO,
                        )
                        .collect {
                          val timestamp = TimeSource.Monotonic.markNow() - start
                          val entry = OutputLine(timestamp, it, OutputType.STDERR)
                          output.invoke(entry)
                        }
                  }

                  if (!process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
                    log(start, "timeout for command exceeded ($timeout)")
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
                      runtime = end.minus(start),
                  )
                } finally {
                  killProcessAndWait(process)
                }
              }
            }

        override fun close() {}
      }

  override fun close() {
    runners.forEach { it.close() }
  }
}

class LocalTestContext : CommandTestContext<LocalCommandBuilder, LocalScriptBuilder> {

  private val resources = mutableListOf<Closeable>()

  override fun command(vararg command: String) =
      LocalCommandBuilder(command.toList().toTypedArray()).apply { resources.add(this) }

  override fun command(command: Path) =
      LocalCommandBuilder(listOf(command.absolutePathString()).toTypedArray()).apply {
        resources.add(this)
      }

  override fun script() = LocalScriptBuilder().apply { resources.add(this) }

  override fun toString(): String = "LocalTestContext()"

  override fun close() {
    resources.forEach { it.close() }
  }
}

fun testLocal() = LocalTestContext()

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
