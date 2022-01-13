package de.solidblocks.agent.base

import de.solidblocks.agent.base.api.BaseAgentApiClient
import de.solidblocks.base.solidblocksVersion
import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DevelopmentEnvironmentExtension::class)
class AgentHttpServerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testEnvironmentCa(developmentEnvironment: DevelopmentEnvironment) {

        val environmentContext = developmentEnvironment.environmentContext()

        val agentHttpServer = AgentHttpServer(environmentContext.serverCertificateManager("test"), environmentContext.clientCaCertificateManager(), -1)

        val client = BaseAgentApiClient(
            "https://localhost:${agentHttpServer.server.actualPort()}",
            environmentContext.clientCertificateManager("test"),
            environmentContext.serverCaCertificateManager()
        )
        assertThat(client.version()?.version).isEqualTo(solidblocksVersion())
    }

    @Test
    fun testTenantCa(developmentEnvironment: DevelopmentEnvironment) {

        val tenantContext = developmentEnvironment.tenantContext()

        val agentHttpServer = AgentHttpServer(tenantContext.serverCertificateManager("test"), tenantContext.clientCaCertificateManager(), -1)

        val client = BaseAgentApiClient(
            "https://localhost:${agentHttpServer.server.actualPort()}",
            tenantContext.clientCertificateManager("test"),
            tenantContext.serverCaCertificateManager()
        )
        assertThat(client.version()?.version).isEqualTo(solidblocksVersion())
    }
}
