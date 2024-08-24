package de.solidblocks.infra.test.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.InspectExecResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.infra.test.*
import de.solidblocks.infra.test.Constants.dockerTestimageLabels
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.*
import java.lang.Thread.sleep
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource


class DockerCommandBuilder(executable: String) : CommandBuilder(executable) {

    private var dockerPullTimout = 5.minutes

    private var sourceDir: Path? = null

    fun dockerPullTimout(timeout: Duration) = apply {
        this.dockerPullTimout = timeout
    }

    fun sourceDir(sourceDir: Path) = apply {
        this.sourceDir = sourceDir
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun runInternal(
        start: TimeSource.Monotonic.ValueTimeMark,
        stdin: Channel<String>,
        output: (entry: OutputLine) -> Unit
    ): Deferred<ProcessResult> =
        withContext(Dispatchers.IO) {
            val dockerClient = createDockerClient()

            var end = TimeSource.Monotonic.markNow()

            pullDockerImage(start, dockerClient)

            val mountDir = if (sourceDir == null) {
                val executablePath = Path.of(executable)
                val tempDir = tempDir()

                if (executablePath.exists()) {
                    tempDir.createFromPath(executablePath).executable().create()
                    executable = tempDir.path.resolve(executablePath.fileName).absolutePathString()
                }

                tempDir.path
            } else {
                sourceDir!!
            }

            val createContainer =
                dockerClient.createContainerCmd("ghcr.io/pellepelster/solidblocks-test-ubuntu-22.04:latest")
                    .withHostConfig(
                        HostConfig.newHostConfig().withMounts(
                            listOf(
                                Mount().withType(MountType.BIND).withSource(mountDir.absolutePathString())
                                    .withTarget(mountDir.absolutePathString())
                            )
                        )
                    ).withLabels(dockerTestimageLabels)
                    .withCmd("sleep", "infinity").exec()
            dockerClient.startContainerCmd(createContainer.id).exec()

            val exec = dockerClient.execCreateCmd(createContainer.id)
                .withCmd(executable).withAttachStderr(true)
                .withAttachStdout(true).withAttachStdin(true).withTty(false).exec()

            val stdinStream = PipedInputStream()
            val stdinWriter = BufferedWriter(OutputStreamWriter(PipedOutputStream(stdinStream)))

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

            val result = async {
                try {
                    withTimeout(timeout) {
                        dockerClient.execStartCmd(exec.id).withStdIn(stdinStream)
                            .exec(object : ResultCallback<Frame> {
                                override fun close() {
                                }

                                override fun onStart(closeable: Closeable?) {
                                    //TODO start = TimeSource.Monotonic.markNow()
                                }

                                override fun onError(throwable: Throwable?) {
                                    cancel("reading output failed", throwable)
                                }

                                override fun onComplete() {
                                    end = TimeSource.Monotonic.markNow()
                                }

                                override fun onNext(frame: Frame) {
                                    val payload = frame.payload.decodeToString()
                                    log(
                                        start, payload, when (frame.streamType) {
                                            StreamType.STDOUT -> LogType.stdout
                                            StreamType.STDERR -> LogType.stderr
                                            else -> {
                                                throw RuntimeException("unsupported docker log stream type: ${frame.streamType}")
                                            }
                                        }
                                    )

                                    payload.lines().dropLastWhile { it.isEmpty() }.forEach {
                                        output.invoke(
                                            OutputLine(
                                                TimeSource.Monotonic.markNow() - start,
                                                it,
                                                when (frame.streamType) {
                                                    StreamType.STDOUT -> OutputType.stdout
                                                    StreamType.STDERR -> OutputType.stderr
                                                    else -> {
                                                        throw RuntimeException("unsupported docker log stream type: ${frame.streamType}")
                                                    }
                                                }
                                            )
                                        )
                                    }
                                }
                            })

                        ProcessResult(
                            waitForExitCode(dockerClient, exec.id).exitCodeLong.toInt(), end - start
                        )
                    }

                } catch (e: TimeoutCancellationException) {
                    log(start, "timeout for command exceeded (${timeout})")
                    dockerClient.killContainerCmd(createContainer.id).exec()

                    ProcessResult(
                        waitForExitCode(dockerClient, exec.id).exitCodeLong.toInt(),
                        end - start
                    )
                } finally {
                    stdin.close()

                    try {
                        dockerClient.stopContainerCmd(createContainer.id).withTimeout(0).exec()
                    } catch (e: NotModifiedException) {
                    }
                    dockerClient.removeContainerCmd(createContainer.id)
                }
            }

            result
        }

    private fun pullDockerImage(start: TimeSource.Monotonic.ValueTimeMark, dockerClient: DockerClient) {
        val image = "ghcr.io/pellepelster/solidblocks-test-ubuntu-22.04:latest"
        log(start, "pulling docker image '${image}")
        dockerClient.pullImageCmd(image)
            .exec(object : PullImageResultCallback() {
                override fun onNext(item: PullResponseItem?) {
                    print("*")
                }
            }).awaitCompletion(dockerPullTimout.inWholeSeconds, TimeUnit.SECONDS)
    }

    private suspend fun CoroutineScope.waitForExitCode(
        dockerClient: DockerClient, execId: String
    ): InspectExecResponse {

        while (dockerClient.inspectExecCmd(execId).exec().exitCodeLong == null) {
            yield()
            sleep(50)
        }

        return dockerClient.inspectExecCmd(execId).exec()
    }

}

class DockerTestContext : TestContext<DockerCommandBuilder> {

    override fun command(executable: Path) = DockerCommandBuilder(executable.absolutePathString())

    override fun toString(): String {
        return "DockerTestContext()"
    }
}

fun docker() = DockerTestContext()

fun createDockerClient(): DockerClient {
    val config: DefaultDockerClientConfig.Builder = DefaultDockerClientConfig.createDefaultConfigBuilder()

    val httpClient = ZerodepDockerHttpClient.Builder()
    httpClient.dockerHost(URI.create("unix:///var/run/docker.sock"))

    val dockerClient: DockerClient =
        DockerClientBuilder.getInstance(config.build()).withDockerHttpClient(httpClient.build()).build()
    return dockerClient
}
