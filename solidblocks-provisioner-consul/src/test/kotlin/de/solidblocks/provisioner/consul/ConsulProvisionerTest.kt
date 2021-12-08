package de.solidblocks.provisioner.consul

import de.solidblocks.provisioner.consul.policy.ConsulPolicy
import de.solidblocks.provisioner.consul.policy.ConsulPolicyProvisioner
import de.solidblocks.provisioner.consul.policy.ConsulRuleBuilder
import de.solidblocks.provisioner.consul.policy.Privileges
import de.solidblocks.provisioner.consul.token.ConsulToken
import de.solidblocks.provisioner.consul.token.ConsulTokenLookup
import de.solidblocks.provisioner.consul.token.ConsulTokenProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@SpringBootTest(classes = [TestApplicationContext::class])
@TestPropertySource(properties = ["vault_addr=http://localhost:8200"])
@Testcontainers
class ConsulProvisionerTest {

    @Autowired
    private lateinit var policyProvisioner: ConsulPolicyProvisioner

    @Autowired
    private lateinit var tokenProvisioner: ConsulTokenProvisioner

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

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("consul.addr") { "http://localhost:${environment.getServicePort("consul", 8500)}" }
        }
    }

    @Test
    fun testPolicyDiffAndApply() {

        val rules = ConsulRuleBuilder().addKeyPrefix("prefix1", Privileges.write)
        val acl = ConsulPolicy("acl1", rules.asPolicy())

        val result = policyProvisioner.diff(acl)
        assertThat(result.result?.missing).isTrue

        policyProvisioner.apply(acl)

        val afterApplyResult = policyProvisioner.diff(acl)
        assertThat(afterApplyResult.result?.missing).isFalse

        // update is always enforced to ensure rules are up-to-date
        assertThat(policyProvisioner.diff(acl).result?.hasChangesOrMissing()).isTrue
    }

    @Test
    fun testTokenDiffAndApply() {

        val rules = ConsulRuleBuilder().addKeyPrefix("prefix1", Privileges.write)
        val acl = ConsulPolicy("token_acl1", rules.asPolicy())
        policyProvisioner.apply(acl)

        val token1Id = UUID.randomUUID()
        val token1 = ConsulToken(token1Id, "token1", listOf(acl))
        tokenProvisioner.apply(token1)

        val token1Lookup = tokenProvisioner.lookup(ConsulTokenLookup(token1Id))
        assertThat(token1Lookup.result!!.id).isEqualTo(token1Id)
    }
}
