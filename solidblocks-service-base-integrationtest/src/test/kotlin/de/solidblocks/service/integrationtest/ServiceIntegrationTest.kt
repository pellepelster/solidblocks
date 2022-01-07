package de.solidblocks.service.integrationtest

import de.solidblocks.core.utils.LinuxCommandExecutor
import de.solidblocks.service.base.VersionResponse
import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import de.solidblocks.test.KDockerComposeContainer
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

@ExtendWith(DevelopmentEnvironmentExtension::class)
class ServiceIntegrationTest {

    private val logger = KotlinLogging.logger {}

    var commandExecutor: LinuxCommandExecutor? = null

    @Test
    fun testServiceBootstrap(developmentEnvironment: DevelopmentEnvironment) {

        val blueVersion =
            ServiceIntegrationTest::class.java.getResource("/service-integrationtest/bootstrap/artefacts/blue.version")
                .readText().trim()
        logger.info { "blue version for integration test is '$blueVersion'" }

        val greenVersion =
            ServiceIntegrationTest::class.java.getResource("/service-integrationtest/bootstrap/artefacts/green.version")
                .readText().trim()
        logger.info { "green version for integration test is '$greenVersion'" }

        val dockerEnvironment =
            KDockerComposeContainer(File("src/test/resources/service-integrationtest/docker-compose.yml"))
                .apply {
                    withBuild(false)
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

        thread {
            val workingDir = System.getProperty("user.dir")
            commandExecutor = LinuxCommandExecutor()
            val result = commandExecutor!!.executeCommand(
                printStream = true,
                environment = mapOf("SOLIDBLOCKS_DIR" to solidblocksDirectory.toString()),
                workingDir = File(workingDir),
                command = listOf("$workingDir/solidblocks-service-wrapper.sh").toTypedArray()
            )

            while (true) {
                result.success
            }
        }

        val client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
        }

        await until {
            runBlocking {
                try {
                    val response: HttpResponse = client.get("http://localhost:8080/v1/version")
                    response.status.isSuccess()
                } catch (e: Exception) {
                    false
                }
            }
        }

        runBlocking {
            val response: VersionResponse = client.get("http://localhost:8080/v1/version")
            assertThat(response.version).isEqualTo(blueVersion)
        }
    }

    private fun createSolidblocksDirectory(vaultToken: String, solidblocksVersion: String, dockerEnvironment: KDockerComposeContainer): Path? {

        val solidblocksDir = Files.createTempDirectory("service-integrationtest")

        val protectedDir = File(solidblocksDir.toFile(), "protected")
        protectedDir.mkdirs()

        val initialEnvironmentFile = File(protectedDir, "initial_environment")
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
            SOLIDBLOCKS_COMPONENT=solidblocks-service-base-integrationtest
            """.trimIndent()
        )
        logger.info { "created service instance file: '$serviceEnvironmentFile'" }

        return solidblocksDir
    }

    @AfterEach
    fun cleanUp() {
        commandExecutor?.kill()
    }
}
