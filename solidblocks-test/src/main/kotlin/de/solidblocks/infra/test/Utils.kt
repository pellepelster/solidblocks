package de.solidblocks.infra.test

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.infra.test.Constants.durationFormat
import java.net.URI
import java.text.DecimalFormat
import kotlin.time.Duration
import kotlin.time.TimeSource

object Constants {
    val dockerTestimageLabels = mapOf("managed-by" to "solidblocks-test")
    val durationFormat = DecimalFormat("000.000")
}

enum class LogType { stdout, stderr, test }

val logTypeMaxLength = LogType.entries.maxOf { it.name.length }

fun log(start: TimeSource.Monotonic.ValueTimeMark, message: String, type: LogType = LogType.test) =
    log(TimeSource.Monotonic.markNow() - start, message, type)

fun log(duration: Duration, message: String, type: LogType = LogType.test) {
    val logType = type.name.padStart(logTypeMaxLength)
    println("${durationFormat.format(duration.inWholeMilliseconds / 1000f)}s [${logType}] ${message}")
}

fun createDockerClient(): DockerClient {
    val config: DefaultDockerClientConfig.Builder = DefaultDockerClientConfig.createDefaultConfigBuilder()

    val httpClient = ZerodepDockerHttpClient.Builder()
    httpClient.dockerHost(URI.create("unix:///var/run/docker.sock"))

    val dockerClient: DockerClient =
        DockerClientBuilder.getInstance(config.build()).withDockerHttpClient(httpClient.build()).build()
    return dockerClient
}
