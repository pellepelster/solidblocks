package de.solidblocks.provisioner.consul.acl

import de.solidblocks.provisioner.consul.policy.ConsulRuleBuilder
import de.solidblocks.provisioner.consul.policy.Privileges
import de.solidblocks.provisioner.consul.policy.Privileges.write
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConsulRuleBuilderTest {

    @Test
    fun testKeyPrefix() {
        val builder = ConsulRuleBuilder().addKeyPrefix("path1", write)
        assertThat(builder.asPolicy()).isEqualTo("key_prefix \"path1\" { policy = \"write\" } \n")
    }

    @Test
    fun testNodePrefix() {
        val builder = ConsulRuleBuilder().addNodePrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("node_prefix \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testEventPrefix() {
        val builder = ConsulRuleBuilder().addEventPrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("event_prefix \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testEvent() {
        val builder = ConsulRuleBuilder().addEvent("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("event \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testAgentPrefix() {
        val builder = ConsulRuleBuilder().addAgentPrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("agent_prefix \"\" { policy = \"read\" } \n")
    }

    @Test
    fun testSessionPrefix() {
        val builder = ConsulRuleBuilder().addSessionPrefix("", Privileges.read)
        assertThat(builder.asPolicy()).isEqualTo("session_prefix \"\" { policy = \"read\" } \n")
    }
}
