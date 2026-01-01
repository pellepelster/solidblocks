package de.solidblocks.infra.test.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectExecResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.PullResponseItem
import de.solidblocks.infra.test.CommandTestContext
import de.solidblocks.infra.test.Constants.dockerTestImageLabels
import de.solidblocks.infra.test.command.CommandBuilder
import de.solidblocks.infra.test.command.CommandRunner
import de.solidblocks.infra.test.command.ProcessResult
import de.solidblocks.infra.test.createDockerClient
import de.solidblocks.infra.test.files.tempDir
import de.solidblocks.infra.test.output.TimestampedOutputLine
import de.solidblocks.utils.logInfo
import java.io.*
import java.lang.Thread.sleep
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

enum class DockerTestImage {
  DEBIAN_10 {
    override fun toString() = "ghcr.io/pellepelster/solidblocks-test-debian-10:latest"
  },
  DEBIAN_11 {
    override fun toString() = "ghcr.io/pellepelster/solidblocks-test-debian-11:latest"
  },
  DEBIAN_12 {
    override fun toString() = "ghcr.io/pellepelster/solidblocks-test-debian-12:latest"
  },
  UBUNTU_20 {
    override fun toString() = "ghcr.io/pellepelster/solidblocks-test-ubuntu-20.04:latest"
  },
  UBUNTU_22 {
    override fun toString() = "ghcr.io/pellepelster/solidblocks-test-ubuntu-22.04:latest"
  },
  UBUNTU_24 {
    override fun toString() = "ghcr.io/pellepelster/solidblocks-test-ubuntu-24.04:latest"
  },
}

class DockerCommandBuilder(private val image: DockerTestImage, command: Array<String>) :
    CommandBuilder(command) {

  private var dockerPullTimout = 5.minutes

  private var sourceDir: Path? = null

  private var tempDirs = mutableListOf<Closeable>()

  fun dockerPullTimout(timeout: Duration) = apply { this.dockerPullTimout = timeout }

  fun sourceDir(sourceDir: Path) = apply { this.sourceDir = sourceDir }

  @OptIn(DelicateCoroutinesApi::class)
  override suspend fun createCommandRunner(
      start: TimeSource.Monotonic.ValueTimeMark,
  ): CommandRunner {
    val dockerClient = createDockerClient()

    var end = TimeSource.Monotonic.markNow()

    pullDockerImage(start, dockerClient)

    val mountDir =
        if (sourceDir == null) {
          val executablePath = Path.of(command.first())
          val tempDir = tempDir()
          tempDirs.add(tempDir)

          if (executablePath.exists()) {
            tempDir.fileFromPath(executablePath).executable().create()
            command[0] = tempDir.path.resolve(executablePath.fileName).absolutePathString()
          }

          tempDir.path
        } else {
          sourceDir!!
        }

    val createContainer =
        dockerClient
            .createContainerCmd(image.toString())
            .withHostConfig(
                HostConfig.newHostConfig()
                    .withMounts(
                        listOf(
                            Mount()
                                .withType(MountType.BIND)
                                .withSource(mountDir.absolutePathString())
                                .withTarget(mountDir.absolutePathString()),
                        ),
                    ),
            )
            .withLabels(dockerTestImageLabels)
            .withCmd("sleep", "infinity")
            .exec()
    dockerClient.startContainerCmd(createContainer.id).exec()

    return object : CommandRunner {

      override suspend fun runCommand(
          command: Array<String>,
          envs: Map<String, String>,
          inheritEnv: Boolean,
          stdin: Channel<String>,
          output: (entry: TimestampedOutputLine) -> Unit,
      ) =
          withContext(Dispatchers.IO) {
            async {
              val stdinStream = PipedInputStream()
              val stdinWriter = BufferedWriter(OutputStreamWriter(PipedOutputStream(stdinStream)))

              val exec =
                  dockerClient
                      .execCreateCmd(createContainer.id)
                      .withCmd(*command)
                      .withAttachStderr(true)
                      .withEnv(envs.map { "${it.key}=${it.value}" })
                      .withAttachStdout(true)
                      .withAttachStdin(true)
                      .withTty(false)
                      .exec()
              logInfo("starting command '${command.joinToString(" ")}'", duration = start - start)

              launch {
                while (!stdin.isClosedForReceive) {
                  val result = stdin.tryReceive()
                  if (result.isSuccess) {
                    result.getOrNull()?.let {
                      stdinWriter.write(it)
                      stdinWriter.newLine()
                      stdinWriter.flush()
                    }
                  }
                  yield()
                }
              }

              try {
                withTimeout(timeout) {
                  dockerClient
                      .execStartCmd(exec.id)
                      .withStdIn(stdinStream)
                      .exec(
                          object : BaseDockerResultCallback(start, output) {
                            override fun onError(throwable: Throwable?) {
                              cancel("reading output failed", throwable)
                            }

                            override fun onComplete() {
                              end = TimeSource.Monotonic.markNow()
                            }
                          },
                      )

                  ProcessResult(
                      waitForExitCode(dockerClient, exec.id).exitCodeLong.toInt(),
                      end - start,
                  )
                }
              } catch (e: TimeoutCancellationException) {
                logInfo(
                    "timeout for command exceeded ($timeout)",
                    start = start,
                )
                dockerClient.killContainerCmd(createContainer.id).exec()

                ProcessResult(
                    waitForExitCode(dockerClient, exec.id).exitCodeLong.toInt(),
                    end - start,
                )
              } finally {
                stdin.close()
              }
            }
          }

      override fun close() {
        try {
          dockerClient.stopContainerCmd(createContainer.id).withTimeout(0).exec()
        } catch (e: NotModifiedException) {}

        dockerClient.removeContainerCmd(createContainer.id)
      }
    }
  }

  override fun close() {
    tempDirs.forEach { it.close() }
  }

  private fun pullDockerImage(
      start: TimeSource.Monotonic.ValueTimeMark,
      dockerClient: DockerClient,
  ) {
    logInfo("pulling docker image '$image", start = start)
    dockerClient
        .pullImageCmd(image.toString())
        .exec(
            object : PullImageResultCallback() {
              override fun onNext(item: PullResponseItem?) {
                // print("*")
              }
            },
        )
        .awaitCompletion(dockerPullTimout.inWholeSeconds, TimeUnit.SECONDS)
  }

  private suspend fun waitForExitCode(
      dockerClient: DockerClient,
      execId: String,
  ): InspectExecResponse {
    while (dockerClient.inspectExecCmd(execId).exec().exitCodeLong == null) {
      yield()
      sleep(50)
    }

    return dockerClient.inspectExecCmd(execId).exec()
  }
}

class DockerTestContext(private val image: DockerTestImage) :
    CommandTestContext<DockerCommandBuilder, DockerScriptBuilder> {

  private val resources = mutableListOf<Closeable>()

  override fun command(vararg command: String) =
      DockerCommandBuilder(image, command.toList().toTypedArray()).apply { resources.add(this) }

  override fun command(command: Path): DockerCommandBuilder =
      DockerCommandBuilder(image, listOf(command.absolutePathString()).toTypedArray()).apply {
        resources.add(this)
      }

  override fun script() = DockerScriptBuilder(image).apply { resources.add(this) }

  override fun toString(): String = "DockerTestContext()"

  override fun close() {
    resources.forEach { it.close() }
  }
}

fun dockerTestContext(image: DockerTestImage) = DockerTestContext(image)
