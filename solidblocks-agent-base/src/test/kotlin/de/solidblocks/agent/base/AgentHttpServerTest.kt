package de.solidblocks.agent.base

import de.solidblocks.agent.base.api.BaseAgentApiClient
import de.solidblocks.base.solidblocksVersion
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AgentHttpServerTest {

    private val logger = KotlinLogging.logger {}

    val agentHttpServer = AgentHttpServer(port = -1)

    @Test
    fun testGetVersion() {

        val client = BaseAgentApiClient("http://localhost:${agentHttpServer.server.actualPort()}")
        assertThat(client.version()?.version).isEqualTo(solidblocksVersion())
    }

    @Test
    @Disabled
    fun testTriggerUpdate() {

        val client = BaseAgentApiClient("http://localhost:${agentHttpServer.server.actualPort()}")
        assertThat(client.triggerUpdate("XXXX")).isEqualTo(solidblocksVersion())
    }
}
