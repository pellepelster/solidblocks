package de.solidblocks.service.base

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.github.dockerjava.api.model.Volume
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import de.solidblocks.base.Constants.SERVICE_LABEL_KEY
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

class DockerManager(
        private val dockerImage: String,
        private val service: String,
        private val storageDir: String,
        private val ports: Set<Int>,
        private val bindings: Map<String, String> = emptyMap(),
        private val healthCheck: Boolean = true,
        private val network: String? = null
) {
    private val client = OkHttpClient()

    private val logger = KotlinLogging.logger {}

    private val dockerClient: DockerClient

    private val retryConfig: RetryConfig.Builder<Boolean> =
        RetryConfig.custom<Boolean>().retryOnResult { it == false }.maxAttempts(20).waitDuration(Duration.ofSeconds(1))

    private val retry: Retry = Retry.of("healthcheck", retryConfig.build())

    init {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()

        dockerClient = DockerClientImpl.getInstance(
                config,
                ZerodepDockerHttpClient.Builder().dockerHost(URI.create("unix:///var/run/docker.sock")).build()
        )
    }


    fun existsImage(imageName: String): Boolean = try {
        dockerClient.inspectImageCmd(imageName).exec()
        true
    } catch (e: NotFoundException) {
        false
    }

    fun start(): Boolean {
        logger.info { "starting docker image '$dockerImage'" }

        if (!existsImage(dockerImage)) {
            try {
                val pullResult = dockerClient.pullImageCmd(dockerImage).start().awaitCompletion(5, TimeUnit.MINUTES)
                if (!pullResult) {
                    logger.error { "failed to pull docker image '$dockerImage'" }
                    return false
                }
            } catch (e: NotFoundException) {
                logger.error(e) { "failed to pull docker image '$dockerImage'" }
                return false
            }
        }

        val hostConfig = HostConfig.newHostConfig()
                .withPortBindings(ports.map { PortBinding(Ports.Binding.empty(), ExposedPort(it)) })
                .withAutoRemove(true)
                .withBinds(
                        bindings.map { Bind(it.key, Volume(it.value)) } +
                                Bind(storageDir, Volume("/storage/local"))
                )

        if (network != null) {
            hostConfig.withNetworkMode(network)
        }

        val result = dockerClient.createContainerCmd(dockerImage)
                .withExposedPorts(ports.map { ExposedPort(it) })
                .withLabels(mapOf(SERVICE_LABEL_KEY to service))
                .withHostConfig(hostConfig).exec()

        dockerClient.startContainerCmd(result.id).exec()

        return waitForRunning() && waitForHealthy()
    }

    fun mappedPort(port: Int) = serviceContainers().flatMap {
        val inspect = dockerClient.inspectContainerCmd(it.id).exec()
        inspect.networkSettings.ports.bindings.entries
    }.filter { it.key.port == port }.map { it.value.map { it.hostPortSpec }.firstOrNull() }.firstOrNull()

    private fun waitForRunning(): Boolean {

        val result = retry.executeCallable {
            logger.info { "waiting for containers for service '$service'" }
            isRunning()
        }

        if (!result) {
            logger.error { "service '$service' not running" }
        } else {
            logger.error { "service '$service' running" }
        }

        return result
    }

    private fun waitForHealthy(): Boolean {
        val result = retry.executeCallable {
            logger.info { "waiting for service '$service' to become healthy" }
            isHealthy()
        }

        if (!result) {
            logger.error { "service '$service' not healthy" }
        } else {
            logger.error { "service '$service' healthy" }
        }

        return result
    }

    private fun serviceContainers() =
        dockerClient.listContainersCmd().exec().filter { it.labels[SERVICE_LABEL_KEY] == service }

    fun stop(): Boolean {
        serviceContainers().forEach {
            logger.info { "stopping container '${it.id}' for service '$service'" }
            dockerClient.stopContainerCmd(it.id).exec()
        }

        return true
    }

    fun isRunning(): Boolean = serviceContainers().isNotEmpty()

    fun isHealthy(): Boolean {

        if (!healthCheck) {
            return true
        }

        return ports.map {
            val address = "http://localhost:${mappedPort(it)}"

            logger.info { "checking health for '$address'" }
            val request = Request.Builder()
                .url(address)
                .build()

            val response = client.newCall(request).execute()

            logger.info { "health for '$address' returned ${response.code}" }

            return response.isSuccessful
        }.all { it }
    }
}
