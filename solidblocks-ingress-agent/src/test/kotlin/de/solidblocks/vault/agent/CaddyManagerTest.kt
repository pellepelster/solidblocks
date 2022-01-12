package de.solidblocks.vault.agent

import de.solidblocks.ingress.agent.CaddyManager
import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import de.solidblocks.test.KDockerComposeContainer
import de.solidblocks.test.TestUtils.initWorldReadableTempDir
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.util.*

@ExtendWith(DevelopmentEnvironmentExtension::class)
class CaddyManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testProvisionIngressForService(developmentEnvironment: DevelopmentEnvironment) {

        val service = "ingress-${UUID.randomUUID()}"
        val tempDir = initWorldReadableTempDir(service)

        val reference = developmentEnvironment.reference.toService(service)
        val certificate =
            developmentEnvironment.createCertificate(developmentEnvironment.reference.toService("backend-service1"))

        val issuingCaFile = File(tempDir, "ca.crt")
        val privateKeyFile = File(tempDir, "server.key")
        val certificateFile = File(tempDir, "server.cert")

        logger.info { "writing ca file to '$issuingCaFile'" }
        issuingCaFile.writeText(certificate!!.issuingCaRaw)

        logger.info { "writing private key to '$privateKeyFile'" }
        privateKeyFile.writeText(certificate.privateKeyRaw)

        logger.info { "writing certificate to '$certificateFile'" }
        certificateFile.writeText(certificate.certificateRaw)

        val caddyManager = CaddyManager(
            reference,
            tempDir,
            issuingCaFile,
            network = "solidblocks-dev"
        )
        assertThat(caddyManager.start()).isTrue

        val dockerEnvironment = KDockerComposeContainer(File("src/test/resources/ingress/docker-compose.yml"))
            .apply {
                withBuild(true)
                    .withEnv(
                        mapOf(
                            "SERVER_CERT" to certificate.certificateRaw,
                            "SERVER_KEY" to certificate.privateKeyRaw,
                        )
                    )
                withExposedService("backend-service1", 443)
            }
        dockerEnvironment.start()

        caddyManager.createReverseProxy("backend-service1.tenant1.dev.local.test:443")

        val request = Request.Builder()
            .url("http://localhost:${caddyManager.httpPort()}/test.txt")
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
