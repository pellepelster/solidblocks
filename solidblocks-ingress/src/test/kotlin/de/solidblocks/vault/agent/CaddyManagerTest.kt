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

        val reference = developmentEnvironment.reference.toService("backend-service1")

        val tenantContext = developmentEnvironment.tenantContext()
        val environmentContext = developmentEnvironment.environmentContext()

        val environmentClientCertificateManager = environmentContext.clientCertificateManager("client1")
        val environmentClientCACertificateManager = environmentContext.clientCaCertificateManager()

        val tenantCACertificateManager = tenantContext.serverCaCertificateManager()

        val clientCertificate = environmentClientCertificateManager.waitForCertificate()

        val serverCaFile = File(tempDir, "ca.crt")
        val clientPrivateKeyFile = File(tempDir, "server.key")
        val clientCertificateFile = File(tempDir, "server.cert")

        logger.info { "writing ca file to '$serverCaFile'" }
        serverCaFile.writeText(tenantCACertificateManager.waitForCaCertificate().caCertificateRaw)

        logger.info { "writing private key to '$clientPrivateKeyFile'" }
        clientPrivateKeyFile.writeText(clientCertificate.privateKeyRaw)

        logger.info { "writing certificate to '$clientCertificateFile'" }
        clientCertificateFile.writeText(clientCertificate.certificateRaw)

        val caddyManager = CaddyManager(
            reference,
            tempDir,
            serverCaFile,
            clientKey = clientPrivateKeyFile,
            clientCert = clientCertificateFile,
            network = "solidblocks-dev"
        )
        assertThat(caddyManager.start()).isTrue

        val tenantCertificateManager = tenantContext.serverCertificateManager("backend-service1")
        val tenantCertificate = tenantCertificateManager.waitForCertificate()

        val dockerEnvironment = KDockerComposeContainer(File("src/test/resources/ingress/docker-compose.yml"))
            .apply {
                withBuild(true)
                    .withEnv(
                        mapOf(
                            "SERVER_CERT" to tenantCertificate.certificateRaw,
                            "SERVER_KEY" to tenantCertificate.privateKeyRaw,
                            "CLIENT_CA_CERT" to environmentClientCACertificateManager.waitForCaCertificate().caCertificateRaw,
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
