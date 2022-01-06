package de.solidblocks.service.integrationtest

import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import de.solidblocks.test.KDockerComposeContainer
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.nio.file.Files

@ExtendWith(DevelopmentEnvironmentExtension::class)
class ServiceIntegrationTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testServiceBootstrap(developmentEnvironment: DevelopmentEnvironment) {

        val blueVersion = ServiceIntegrationTest::class.java.getResource("/service-integrationtest/bootstrap/artefacts/blue.version").readText().trim()
        logger.info { "blue version for integration test is '${blueVersion}'" }

        val greenVersion = ServiceIntegrationTest::class.java.getResource("/service-integrationtest/bootstrap/artefacts/green.version").readText().trim()
        logger.info { "green version for integration test is '${greenVersion}'" }

        val dockerEnvironment = KDockerComposeContainer(File("src/test/resources/service-integrationtest/docker-compose.yml"))
                .apply {
                    withBuild(false)
                            .withEnv(mapOf(
                                    "SOLIDBLOCKS_BLUE_VERSION" to blueVersion,
                                    "SOLIDBLOCKS_GREEN_VERSION" to greenVersion
                            ))
                    withExposedService("bootstrap", 80)
                }
        dockerEnvironment.start()

        val vaultToken = developmentEnvironment.createService("test")
        val tempDir = Files.createTempDirectory("service-integrationtest")


        val protectedDir = File(tempDir.toFile(), "protected")
        protectedDir.mkdirs()

        val initialEnvironmentFile = File(protectedDir, "initial_environment")
        initialEnvironmentFile.writeText("""
        VAULT_TOKEN=${vaultToken}
        GITHUB_TOKEN_RO=XXX
        GITHUB_USERNAME=YYY
        """.trimIndent())
        logger.info { "created initial environment file: '${initialEnvironmentFile}'" }


        val instanceDir = File(tempDir.toFile(), "instance")
        instanceDir.mkdirs()

        val instanceEnvironmentFile = File(instanceDir, "environment")
        instanceEnvironmentFile.writeText("""
        SOLIDBLOCKS_VERSION=${blueVersion}
        SOLIDBLOCKS_BOOTSTRAP_ADDRESS=http://localhost:${dockerEnvironment.getServicePort("bootstrap", 80)}
        """.trimIndent())
        logger.info { "created environment instance file: '${instanceEnvironmentFile}'" }

        val serviceDir = File(tempDir.toFile(), "service")
        serviceDir.mkdirs()

        val serviceEnvironmentFile = File(serviceDir, "environment")
        serviceEnvironmentFile.writeText("""
        SOLIDBLOCKS_COMPONENT=solidblocks-service-base-integrationtest
        """.trimIndent())
        logger.info { "created service instance file: '${serviceEnvironmentFile}'" }


        val request = Request.Builder()
                .url("http://localhost:${dockerEnvironment.getServicePort("bootstrap", 80)}/test.txt")
                .build()

        await untilCallTo {
            try {
                OkHttpClient().newCall(request).execute().body!!.string().trim()
            } catch (e: Exception) {
                null
            }
        } matches {
            it == "Hello World!"
        }
    }
}
