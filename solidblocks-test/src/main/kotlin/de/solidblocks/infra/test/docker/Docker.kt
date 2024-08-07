package de.solidblocks.infra.test.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import de.solidblocks.infra.test.*
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.lang.Thread.sleep
import java.nio.file.Path
import java.util.*
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource


class DockerCommandBuilder(executable: String) : CommandBuilder(executable) {

    override fun runInternal() = runBlocking {
        val start = TimeSource.Monotonic.markNow()
        val output: Queue<OutputLine> = LinkedList()

        val config: DefaultDockerClientConfig.Builder =
            DefaultDockerClientConfig.createDefaultConfigBuilder()

        val dockerClient: DockerClient = DockerClientBuilder.getInstance(config.build()).build()


        dockerClient.pullImageCmd("ghcr.io/pellepelster/solidblocks-test-ubuntu-22.04:latest")
            .exec(object : PullImageResultCallback() {
                override fun onNext(item: PullResponseItem?) {
                }
            }).awaitCompletion()

        val tempDir = tempDir()

        val executablePath = Path.of(executable)
        if (executablePath.exists()) {
            tempDir.createFromPath(executablePath).executable().create()
        }

        val createContainer =
            dockerClient.createContainerCmd("ghcr.io/pellepelster/solidblocks-test-ubuntu-22.04:latest")
                .withHostConfig(
                    HostConfig.newHostConfig().withMounts(
                        listOf(
                            Mount().withType(MountType.BIND)
                                .withSource(tempDir.path.absolutePathString())
                                .withTarget("/test")
                        )
                    )
                )
                .withCmd("sleep", "infinity").exec()
        dockerClient.startContainerCmd(createContainer.id).exec()

        val exec = dockerClient.execCreateCmd(createContainer.id)
            .withCmd(executablePath?.fileName?.name?.let { "/test/${it}" } ?: executable)
            .withAttachStderr(true)
            .withAttachStdout(true)
            .withAttachStdin(true)
            .withTty(false).exec()

        dockerClient.execStartCmd(exec.id).exec(object : ResultCallback<Frame> {
            override fun close() {
                log(start, "close")
            }

            override fun onStart(closeable: Closeable?) {
                log(start, "onStart")
            }

            override fun onError(throwable: Throwable?) {
                log(start, "onError")
                throwable?.printStackTrace()
            }

            override fun onComplete() {
                log(start, "onComplete")
            }

            override fun onNext(frame: Frame) {
                val payload = frame.payload.decodeToString()

                payload.lines().dropLastWhile { it.isEmpty() }.forEach {
                    output.add(
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

        val unmatchedWaitForOutput = waitForOutput1(start, waitForOutput, output)


        while (dockerClient.inspectExecCmd(exec.id).exec().exitCodeLong == null) {
            sleep(250)
        }
        val response = dockerClient.inspectExecCmd(exec.id).exec()

        log(start, "stopContainerCmd")
        dockerClient.stopContainerCmd(createContainer.id).withTimeout(0).exec()
        log(start, "removeContainerCmd")
        dockerClient.removeContainerCmd(createContainer.id)
        log(start, "done")

        CommandRunResult(
            unmatchedWaitForOutput,
            CommandResult(response.exitCodeLong.toInt(), 1.seconds, output.toList())
        )
    }
}

class DockerTestContext : TestContext {

    override fun command(executable: Path) = DockerCommandBuilder(executable.absolutePathString())

    override fun toString(): String {
        return "DockerTestContext()"
    }
}

fun docker(): TestContext = DockerTestContext()
