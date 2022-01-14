package de.solidblocks.helloworld.agent

import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

class PlaygroundTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testSandbox1() {
    }
    @Test
    @Disabled
    fun testSandbox() {

        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withRegistryUsername("xxx")
            .withRegistryPassword("xxx")
            .build()
        val dockerClient = DockerClientImpl.getInstance(
            config,
            ZerodepDockerHttpClient.Builder().dockerHost(URI.create("unix:///var/run/docker.sock")).build()
        )

        val pullResult = dockerClient.pullImageCmd("ghcr.io/pellepelster/solidblocks-helloworld:SNAPSHOT-20220111230028").start().awaitCompletion(5, TimeUnit.MINUTES)
        assertThat(pullResult).isTrue
    }
}
