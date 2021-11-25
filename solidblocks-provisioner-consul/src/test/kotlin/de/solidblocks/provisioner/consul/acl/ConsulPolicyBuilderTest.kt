package de.solidblocks.provisioner.consul.acl

import de.solidblocks.provisioner.consul.acl.Privileges.write
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConsulPolicyBuilderTest {

    @Test
    fun testKeyPrefix() {
        val builder = ConsulPolicyBuilder().addKeyPrefix("path1", write)
        assertThat(builder.asPolicy()).isEqualTo("key_prefix \"path1\" { policy = \"write\" } \n")
    }

    @Test
    fun testNodePrefix() {
        val builder = ConsulPolicyBuilder().addNodePrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("node_prefix \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testEventPrefix() {
        val builder = ConsulPolicyBuilder().addEventPrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("event_prefix \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testEvent() {
        val builder = ConsulPolicyBuilder().addEvent("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("event \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testAgentPrefix() {
        val builder = ConsulPolicyBuilder().addAgentPrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("agent_prefix \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testSessionPrefix() {
        val builder = ConsulPolicyBuilder().addSessionPrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("session_prefix \"\" { policy = \"read\" } \n")
    }
}