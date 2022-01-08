package de.solidblocks.helloworld.agent

import de.solidblocks.agent.base.TriggerUpdateRequest
import de.solidblocks.agent.base.TriggerUpdateResponse
import de.solidblocks.agent.base.VersionResponse
import de.solidblocks.base.http.HttpClient
import de.solidblocks.base.http.HttpResponse
import de.solidblocks.core.utils.LinuxCommandExecutor
import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import de.solidblocks.test.KDockerComposeContainer
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreException
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.net.ConnectException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class AgentWrapperProcess(solidblocksDirectory: Path) {
    private val logger = KotlinLogging.logger {}

    val commandExecutor: LinuxCommandExecutor = LinuxCommandExecutor()
    val stopThread = AtomicBoolean(false)
    var thread: Thread = thread {
        while (!stopThread.get()) {
            val workingDir = System.getProperty("user.dir")
            val result = commandExecutor.executeCommand(
                printStream = true,
                environment = mapOf("SOLIDBLOCKS_DIR" to solidblocksDirectory.toString()),
                workingDir = File(workingDir),
                command = listOf("$workingDir/solidblocks-service-wrapper.sh").toTypedArray()
            )

            logger.info { "service wrapper exited with ${result.code}" }
        }
    }

    fun stop() {
        stopThread.set(true)
        commandExecutor.kill()
        thread.join(10000)
    }
}

@ExtendWith(DevelopmentEnvironmentExtension::class)
class HelloWorldAgentIntegrationTest {

    private val logger = KotlinLogging.logger {}

    private var agentWrapperProcess: AgentWrapperProcess? = null

    private val blueVersion =
        HelloWorldAgentIntegrationTest::class.java.getResource("/helloworld/bootstrap/artefacts/blue.version")
            .readText().trim()

    private val greenVersion =
        HelloWorldAgentIntegrationTest::class.java.getResource("/helloworld/bootstrap/artefacts/green.version")
            .readText().trim()

    @Test
    fun testAgentUpdate(developmentEnvironment: DevelopmentEnvironment) {
        logger.info { "blue version for integration test is '$blueVersion'" }
        logger.info { "green version for integration test is '$greenVersion'" }

        val dockerEnvironment =
            KDockerComposeContainer(File("src/test/resources/helloworld/docker-compose.yml"))
                .apply {
                    withBuild(true)
                        .withEnv(
                            mapOf(
                                "SOLIDBLOCKS_BLUE_VERSION" to blueVersion,
                                "SOLIDBLOCKS_GREEN_VERSION" to greenVersion
                            )
                        )
                    withExposedService("bootstrap", 80)
                }
        dockerEnvironment.start()

        val vaultToken = developmentEnvironment.createService("test")
        val solidblocksDirectory = createSolidblocksDirectory(vaultToken, blueVersion, dockerEnvironment)

        agentWrapperProcess = AgentWrapperProcess(solidblocksDirectory)

        val client = HttpClient("http://localhost:8080")

        await ignoreException (ConnectException::class) until {
            val response: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
            response.isSuccessful
        }

        val beforeUpdateVersionResponse: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
        assertThat(beforeUpdateVersionResponse.data?.version).isEqualTo(blueVersion)

        val triggerResponse: HttpResponse<TriggerUpdateResponse> =
            client.post("/v1/agent/trigger-update", TriggerUpdateRequest(greenVersion))
        assertThat(triggerResponse.data?.triggered).isTrue

        await atMost(Duration.ofSeconds(15)) ignoreException (ConnectException::class) untilAsserted {
            val afterUpdateVersionResponse: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
            assertThat(afterUpdateVersionResponse.data?.version).isEqualTo(greenVersion)
        }
    }

    @Test
    fun testAgentUpdateInvalidVersion(developmentEnvironment: DevelopmentEnvironment) {
        logger.info { "blue version for integration test is '$blueVersion'" }
        logger.info { "green version for integration test is '$greenVersion'" }

        val dockerEnvironment =
            KDockerComposeContainer(File("src/test/resources/helloworld/docker-compose.yml"))
                .apply {
                    withBuild(true)
                        .withEnv(
                            mapOf(
                                "SOLIDBLOCKS_BLUE_VERSION" to blueVersion,
                                "SOLIDBLOCKS_GREEN_VERSION" to greenVersion
                            )
                        )
                    withExposedService("bootstrap", 80)
                }
        dockerEnvironment.start()

        val vaultToken = developmentEnvironment.createService("test")
        val solidblocksDirectory = createSolidblocksDirectory(vaultToken, blueVersion, dockerEnvironment)

        agentWrapperProcess = AgentWrapperProcess(solidblocksDirectory)

        val client = HttpClient("http://localhost:8080")

        await ignoreException (ConnectException::class) until {
            val response: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
            response.isSuccessful
        }

        val beforeUpdateVersionResponse: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
        assertThat(beforeUpdateVersionResponse.data?.version).isEqualTo(blueVersion)

        val triggerResponse: HttpResponse<TriggerUpdateResponse> =
            client.post("/v1/agent/trigger-update", TriggerUpdateRequest("invalid-version"))
        assertThat(triggerResponse.data?.triggered).isTrue

        await withPollDelay(Duration.ofSeconds(20)) atMost(Duration.ofSeconds(25)) ignoreException (ConnectException::class) untilAsserted {
            val afterUpdateVersionResponse: HttpResponse<VersionResponse> = client.get("/v1/agent/version")
            assertThat(afterUpdateVersionResponse.data?.version).isEqualTo(blueVersion)
        }
    }

    private fun createSolidblocksDirectory(
        vaultToken: String,
        solidblocksVersion: String,
        dockerEnvironment: KDockerComposeContainer
    ): Path {

        val solidblocksDir = Files.createTempDirectory("helloworld")

        val protectedDir = File(solidblocksDir.toFile(), "protected")
        protectedDir.mkdirs()

        val initialEnvironmentFile = File(protectedDir, "environment")
        initialEnvironmentFile.writeText(
            """
            VAULT_TOKEN=$vaultToken
            GITHUB_TOKEN_RO=XXX
            GITHUB_USERNAME=YYY
            """.trimIndent()
        )
        logger.info { "created initial environment file: '$initialEnvironmentFile'" }

        val instanceDir = File(solidblocksDir.toFile(), "instance")
        instanceDir.mkdirs()

        val instanceEnvironmentFile = File(instanceDir, "environment")
        instanceEnvironmentFile.writeText(
            """
            SOLIDBLOCKS_VERSION=$solidblocksVersion
            SOLIDBLOCKS_BOOTSTRAP_ADDRESS=http://localhost:${dockerEnvironment.getServicePort("bootstrap", 80)}
            """.trimIndent()
        )
        logger.info { "created environment instance file: '$instanceEnvironmentFile'" }

        val serviceDir = File(solidblocksDir.toFile(), "service")
        serviceDir.mkdirs()

        val serviceEnvironmentFile = File(serviceDir, "environment")
        serviceEnvironmentFile.writeText(
            """
            SOLIDBLOCKS_COMPONENT=solidblocks-helloworld-agent
            """.trimIndent()
        )
        logger.info { "created service instance file: '$serviceEnvironmentFile'" }

        return solidblocksDir
    }

    @AfterEach
    fun cleanUp() {
        agentWrapperProcess?.stop()
    }
}
