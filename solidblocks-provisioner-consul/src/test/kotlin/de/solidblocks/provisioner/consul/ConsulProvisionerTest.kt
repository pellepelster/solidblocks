package de.solidblocks.provisioner.consul

import com.orbitz.consul.Consul
import de.solidblocks.provisioner.consul.kv.ConsulKv
import de.solidblocks.provisioner.consul.kv.ConsulKvProvisioner
import de.solidblocks.provisioner.consul.policy.ConsulPolicy
import de.solidblocks.provisioner.consul.policy.ConsulPolicyProvisioner
import de.solidblocks.provisioner.consul.policy.ConsulRuleBuilder
import de.solidblocks.provisioner.consul.policy.Privileges
import de.solidblocks.provisioner.consul.token.ConsulToken
import de.solidblocks.provisioner.consul.token.ConsulTokenLookup
import de.solidblocks.provisioner.consul.token.ConsulTokenProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@Testcontainers
class ConsulProvisionerTest {
    companion object {
        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withExposedService("consul", 8500)
                        .waitingFor(
                            "consul",
                            LogMessageWaitStrategy()
                                .withRegEx(".*federation state pruning.*")
                        )
                        .start()
                }

        private fun consulHttpAddress() = "http://localhost:${environment.getServicePort("consul", 8500)}"

        fun consulClient(): Consul = Consul.builder().withUrl(consulHttpAddress()).withTokenAuth("master-token").build()
    }

    @Test
    fun testPolicyDiffAndApply() {

        val policyProvisioner = ConsulPolicyProvisioner(consulClient())

        val rules = ConsulRuleBuilder().addKeyPrefix("prefix1", Privileges.write)
        val acl = ConsulPolicy("acl1", rules.asPolicy())

        val result = policyProvisioner.diff(acl)
        assertThat(result.result?.missing).isTrue

        policyProvisioner.apply(acl)

        val afterApplyDiff = policyProvisioner.diff(acl)
        assertThat(afterApplyDiff.result?.missing).isFalse

        // update is always enforced to ensure rules are up-to-date
        assertThat(policyProvisioner.diff(acl).result?.needsApply()).isTrue
    }

    @Test
    fun testKvDiffAndApply() {

        val kvProvisioner = ConsulKvProvisioner(consulClient())

        val kv = ConsulKv("consul/key/path")

        val result = kvProvisioner.diff(kv)
        assertThat(result.result?.missing).isTrue

        kvProvisioner.apply(kv)

        val afterApplyDiff = kvProvisioner.diff(kv)
        assertThat(afterApplyDiff.result?.needsApply()).isFalse
    }

    @Test
    fun testTokenDiffAndApply() {
        val policyProvisioner = ConsulPolicyProvisioner(consulClient())

        val rules = ConsulRuleBuilder().addKeyPrefix("prefix1", Privileges.write)
        val acl = ConsulPolicy("token_acl1", rules.asPolicy())
        policyProvisioner.apply(acl)

        val token1Id = UUID.randomUUID()
        val token1 = ConsulToken(token1Id, "token1", setOf(acl))

        val tokenProvisioner = ConsulTokenProvisioner(consulClient())

        val token1FailedLookup = tokenProvisioner.lookup(ConsulTokenLookup(token1Id))
        assertThat(token1FailedLookup.failed).isTrue()

        tokenProvisioner.apply(token1)

        val token1Lookup = tokenProvisioner.lookup(ConsulTokenLookup(token1Id))
        assertThat(token1Lookup.result!!.id).isEqualTo(token1Id)
        assertThat(token1Lookup.result!!.token).isNotEmpty()
    }
}
