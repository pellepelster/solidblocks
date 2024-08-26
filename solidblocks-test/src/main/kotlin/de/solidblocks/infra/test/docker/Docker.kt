package de.solidblocks.infra.test.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectExecResponse
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.infra.test.CommandBuilder
import de.solidblocks.infra.test.Constants.dockerTestimageLabels
import de.solidblocks.infra.test.ProcessResult
import de.solidblocks.infra.test.TestContext
import de.solidblocks.infra.test.command.CommandRunner
import de.solidblocks.infra.test.log
import de.solidblocks.infra.test.output.OutputLine
import de.solidblocks.infra.test.tempDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.Thread.sleep
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource


class DockerCommandBuilder(command: Array<String>) : CommandBuilder(command) {

    private var dockerPullTimout = 5.minutes

    private var sourceDir: Path? = null

    fun dockerPullTimout(timeout: Duration) = apply {
        this.dockerPullTimout = timeout
    }

    fun sourceDir(sourceDir: Path) = apply {
        this.sourceDir = sourceDir
    }

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun createCommandRunner(
        start: TimeSource.Monotonic.ValueTimeMark,
    ): CommandRunner {
        val dockerClient = createDockerClient()

        var end = TimeSource.Monotonic.markNow()

        pullDockerImage(start, dockerClient)

        val mountDir = if (sourceDir == null) {
            val executablePath = Path.of(command.first())
            val tempDir = tempDir()

            if (executablePath.exists()) {
                tempDir.createFromPath(executablePath).executable().create()
                command[0] = tempDir.path.resolve(executablePath.fileName).absolutePathString()
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

        return object : CommandRunner {
            override suspend fun runCommand(
                command: Array<String>,
                envs: Map<String, String>,
                stdin: Channel<String>,
                output: (entry: OutputLine) -> Unit,
            ) = withContext(Dispatchers.IO) {
                async {
                    val stdinStream = PipedInputStream()
                    val stdinWriter = BufferedWriter(OutputStreamWriter(PipedOutputStream(stdinStream)))

                    val exec = dockerClient.execCreateCmd(createContainer.id)
                        .withCmd(*command).withAttachStderr(true)
                        .withEnv(envs.map { "${it.key}=${it.value}" })
                        .withAttachStdout(true).withAttachStdin(true).withTty(false).exec()
                    log(start - start, "starting command '${command.joinToString(" ")}'")

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
                            dockerClient.execStartCmd(exec.id).withStdIn(stdinStream)
                                .exec(object : BaseDockerResultCallback(start, output) {
                                    override fun onError(throwable: Throwable?) {
                                        cancel("reading output failed", throwable)
                                    }

                                    override fun onComplete() {
                                        end = TimeSource.Monotonic.markNow()
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
                    }
                }
            }

            override fun cleanup() {
                try {
                    dockerClient.stopContainerCmd(createContainer.id).withTimeout(0).exec()
                } catch (e: NotModifiedException) {
                }

                dockerClient.removeContainerCmd(createContainer.id)
            }
        }
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

    override fun command(vararg command: String) = DockerCommandBuilder(command.toList().toTypedArray())

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
