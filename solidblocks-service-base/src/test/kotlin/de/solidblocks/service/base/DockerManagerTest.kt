package de.solidblocks.service.base

import de.solidblocks.test.TestUtils.initWorldReadableTempDir
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class DockerManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testStartStopService() {

        val service = "docker-manager-${UUID.randomUUID()}"

        val tempDir = initWorldReadableTempDir(service)
        val webDir = initWorldReadableTempDir(service)

        File(webDir, "test.txt").writeText("Hello World!")

        val dockerManager = DockerManager(
            "halverneus/static-file-server",
            service,
            tempDir,
            setOf(8080),
            mapOf(webDir to "/web")
        )

        assertThat(dockerManager.start()).isTrue
        assertThat(dockerManager.isRunning()).isTrue
        assertThat(dockerManager.isHealthy()).isTrue

        val httpClient = OkHttpClient()
        val request = Request.Builder()
            .url("http://localhost:${dockerManager.mappedPort(8080)}/test.txt")
            .build()
        assertThat(httpClient.newCall(request).execute().body!!.string()).isEqualTo("Hello World!")

        dockerManager.stop()
        assertThat(dockerManager.isRunning()).isFalse
    }
}
