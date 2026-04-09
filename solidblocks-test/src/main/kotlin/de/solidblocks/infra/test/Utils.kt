package de.solidblocks.infra.test

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import java.text.DecimalFormat

object Constants {
    val dockerTestImageLabels = mapOf("managed-by" to "solidblocks-test")
    val durationFormat = DecimalFormat("000.000")
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

fun generateRandomString(length: Int = 12): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { chars.random() }.joinToString("")
}

fun generateTestId(length: Int = 12) = generateRandomString(length).generateTestId(length)

fun String.generateTestId(length: Int = 12): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val alphanumeric = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val base = alphanumeric.length

    val hashBytes = digest.digest(this.toByteArray())
    var number = BigInteger(1, hashBytes)

    val sb = StringBuilder()

    repeat(length) {
        val remainder = (number.mod(BigInteger.valueOf(base.toLong()))).toInt()
        sb.append(alphanumeric[remainder])
        number = number.divide(BigInteger.valueOf(base.toLong()))
    }

    return sb.toString()
}

fun testLabels(testId: String) = mapOf("blcks.de/test-id" to testId, "blcks.de/managed-by" to "test")
