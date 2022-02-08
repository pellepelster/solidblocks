package de.solidblocks.vault.agent

import de.soldiblocks.ingress.api.ServiceIngressRequest
import de.solidblocks.base.defaultHttpClient
import de.solidblocks.ingress.agent.IngressManager
import de.solidblocks.test.IntegrationTestEnvironment
import de.solidblocks.test.IntegrationTestExtension
import de.solidblocks.test.KDockerComposeContainer
import de.solidblocks.test.TestUtils.initWorldReadableTempDir
import mu.KotlinLogging
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.util.*

@ExtendWith(IntegrationTestExtension::class)
class IngressManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testProvisionIngressForService(integrationTestEnvironment: IntegrationTestEnvironment) {

        val service = "ingress-${UUID.randomUUID()}"
        val tempDir = initWorldReadableTempDir(service)

        val tenantContext = integrationTestEnvironment.tenantContext()
        val environmentContext = integrationTestEnvironment.environmentContext()

        val environmentClientCACertificateManager = environmentContext.clientCaCertificateManager()

        // vaultAddressOverride ?: VaultConstants.vaultAddress(environment)

        val ingressManager = IngressManager(
            vaultAddress = integrationTestEnvironment.vaultAddress,
            vaultToken = integrationTestEnvironment.vaultRootToken,
            integrationTestEnvironment.reference.toEnvironmentService("ingress"),
            tempDir,
            network = "solidblocks-dev"
        )
        assertThat(ingressManager.start()).isTrue

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

        ingressManager.updateServices(
            listOf(
                ServiceIngressRequest(
                    integrationTestEnvironment.reference.toService("backend-service1"),
                    hostnames = listOf("localhost"),
                    upstream = "backend-service1.tenant1.dev.local.test:443"
                )
            )
        )

        val request = Request.Builder()
            .url("http://localhost:${ingressManager.httpPort()}/test.txt")
            .build()

        await untilCallTo {
            try {
                defaultHttpClient().newCall(request).execute().body!!.string().trim()
            } catch (e: Exception) {
                null
            }
        } matches {
            it == "Hello World!"
        }
    }
}
