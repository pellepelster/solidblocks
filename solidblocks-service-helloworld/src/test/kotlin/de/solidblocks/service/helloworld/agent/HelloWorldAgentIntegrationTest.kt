package de.solidblocks.service.helloworld.agent

import de.solidblocks.agent.base.api.BaseAgentApiClient
import de.solidblocks.core.utils.LinuxCommandExecutor
import de.solidblocks.test.IntegrationTestEnvironment
import de.solidblocks.test.IntegrationTestExtension
import de.solidblocks.test.KDockerComposeContainer
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.*
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
                command = listOf("$workingDir/../solidblocks-cloud-init/src/bin/solidblocks-agent-wrapper.sh").toTypedArray()
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

@ExtendWith(IntegrationTestExtension::class)
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
    fun testAgentUpdate(integrationTestEnvironment: IntegrationTestEnvironment) {
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

        val vaultToken = integrationTestEnvironment.createService("test")
        val solidblocksDirectory =
            createSolidblocksDirectory(vaultToken, blueVersion, dockerEnvironment, integrationTestEnvironment)

        agentWrapperProcess = AgentWrapperProcess(solidblocksDirectory)

        val client = BaseAgentApiClient(
            "https://localhost:8080",
            integrationTestEnvironment.environmentContext().clientCertificateManager("test"),
            integrationTestEnvironment.tenantContext().serverCaCertificateManager()
        )

        await ignoreException (ConnectException::class) until {
            client.version() != null
        }

        assertThat(client.version()?.version).isEqualTo(blueVersion)
        assertThat(client.triggerUpdate(greenVersion)).isTrue

        await atMost (Duration.ofSeconds(15)) ignoreException (ConnectException::class) untilAsserted {
            assertThat(client.version()?.version).isEqualTo(greenVersion)
        }
    }

    @Test
    fun testAgentUpdateInvalidVersion(integrationTestEnvironment: IntegrationTestEnvironment) {
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

        val vaultToken = integrationTestEnvironment.createService("test")
        val solidblocksDirectory =
            createSolidblocksDirectory(vaultToken, blueVersion, dockerEnvironment, integrationTestEnvironment)

        agentWrapperProcess = AgentWrapperProcess(solidblocksDirectory)

        val client = BaseAgentApiClient(
            "https://localhost:8080",
            integrationTestEnvironment.environmentContext().clientCertificateManager("test"),
            integrationTestEnvironment.tenantContext().serverCaCertificateManager()
        )

        await ignoreException (ConnectException::class) until {
            client.version() != null
        }

        assertThat(client.version()?.version).isEqualTo(blueVersion)
        assertThat(client.triggerUpdate("invalid-version")).isTrue

        await withPollDelay (Duration.ofSeconds(20)) atMost (Duration.ofSeconds(25)) ignoreException (ConnectException::class) untilAsserted {
            assertThat(client.version()?.version).isEqualTo(blueVersion)
        }
    }

    private fun createSolidblocksDirectory(
        vaultToken: String,
        solidblocksVersion: String,
        dockerEnvironment: KDockerComposeContainer,
        integrationTestEnvironment: IntegrationTestEnvironment
    ): Path {

        val solidblocksDir = Files.createTempDirectory("helloworld")

        val protectedDir = File(solidblocksDir.toFile(), "protected")
        protectedDir.mkdirs()

        val initialEnvironmentFile = File(protectedDir, "environment")
        initialEnvironmentFile.writeText(
            """
            VAULT_TOKEN=$vaultToken
            VAULT_ADDR=${integrationTestEnvironment.vaultAddress}
            GITHUB_TOKEN_RO=5c94d4bc-7259-11ec-b135-fb9e235ad033
            GITHUB_USERNAME=pellepelster
            """.trimIndent()
        )
        logger.info { "created initial environment file: '$initialEnvironmentFile'" }

        val instanceDir = File(solidblocksDir.toFile(), "instance")
        instanceDir.mkdirs()

        val downloadDir = File(solidblocksDir.toFile(), "download")
        downloadDir.mkdirs()

        val instanceEnvironmentFile = File(instanceDir, "environment")
        instanceEnvironmentFile.writeText(
            """
            SOLIDBLOCKS_CERTIFICATE_ALT_NAMES=localhost
            SOLIDBLOCKS_CLOUD=${integrationTestEnvironment.reference.cloud}
            SOLIDBLOCKS_ENVIRONMENT=${integrationTestEnvironment.reference.environment}
            SOLIDBLOCKS_TENANT=${integrationTestEnvironment.reference.tenant}
            SOLIDBLOCKS_SERVICE=solidblocks-service-helloworld
            SOLIDBLOCKS_ROOT_DOMAIN=${integrationTestEnvironment.rootDomain}
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
