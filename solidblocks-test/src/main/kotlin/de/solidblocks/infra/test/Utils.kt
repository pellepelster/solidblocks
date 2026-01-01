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
    val dockerTestImageLabels = mapOf("managed-by" to "solidblocks-test")
    val durationFormat = DecimalFormat("000.000")
}

enum class LogType {
    STDOUT,
    STDERR,
    INFO,
    ERROR,
    WARNING,
    CLOUDINIT,
    DMESG,
}

val logTypeMaxLength = LogType.entries.maxOf { it.name.length }

fun log(start: TimeSource.Monotonic.ValueTimeMark, message: String, type: LogType = LogType.INFO) =
    log(TimeSource.Monotonic.markNow() - start, message, type)

fun log(duration: Duration, message: String, type: LogType = LogType.INFO) {
    val logType = type.name.padStart(logTypeMaxLength)
    println("${durationFormat.format(duration.inWholeMilliseconds / 1000f)}s [$logType] $message")
}

fun log(message: String, type: LogType = LogType.INFO) {
    val logType = type.name.padStart(logTypeMaxLength)
    println("[$logType] $message")
}

fun createDockerClient(): DockerClient {
    val config: DefaultDockerClientConfig.Builder =
        DefaultDockerClientConfig.createDefaultConfigBuilder()

    val httpClient = ZerodepDockerHttpClient.Builder()
    httpClient.dockerHost(URI.create("unix:///var/run/docker.sock"))

    val dockerClient: DockerClient =
        DockerClientBuilder.getInstance(config.build())
            .withDockerHttpClient(httpClient.build())
            .build()
    return dockerClient
}

data class GolangPlatform(val os: String, val arch: String)

fun detectGolangPlatform(): GolangPlatform {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()

    val os =
        when {
            osName.contains("mac") || osName.contains("darwin") -> "darwin"
            osName.contains("win") -> "windows"
            osName.contains("nux") || osName.contains("nix") -> "linux"
            osName.contains("freebsd") -> "freebsd"
            osName.contains("openbsd") -> "openbsd"
            osName.contains("solaris") || osName.contains("sunos") -> "solaris"
            else -> throw RuntimeException("unsupported os '$osName'")
        }

    val arch =
        when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "arm64"
            osArch.contains("amd64") || osArch.contains("x86_64") -> "amd64"
            osArch.contains("arm") -> "arm"
            osArch == "x86" || osArch == "i386" || osArch == "i686" -> "386"
            else -> throw RuntimeException("unsupported architecture '$osArch'")
        }

    return GolangPlatform(os, arch)
}
