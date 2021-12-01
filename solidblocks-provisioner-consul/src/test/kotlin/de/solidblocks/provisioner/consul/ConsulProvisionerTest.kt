package de.solidblocks.provisioner.consul

import de.solidblocks.provisioner.consul.policy.ConsulPolicy
import de.solidblocks.provisioner.consul.policy.ConsulPolicyProvisioner
import de.solidblocks.provisioner.consul.policy.ConsulRuleBuilder
import de.solidblocks.provisioner.consul.policy.Privileges
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.io.File

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@SpringBootTest(classes = [TestConfiguration::class], properties = ["spring.main.allow-bean-definition-overriding=true"])
class ConsulProvisionerTest {

    @Autowired
    private lateinit var provisioner: ConsulPolicyProvisioner

    @ClassRule
    var environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                    .apply {
                        withExposedService("consul", 8500)
                                .waitingFor("consul", LogMessageWaitStrategy().withRegEx(".*federation state pruning.*"))
                        start()
                    }


    @Test
    fun testAclDiffAndApply() {

        val rules = ConsulRuleBuilder().addKeyPrefix("prefix1", Privileges.write)
        val acl = ConsulPolicy("acl1", rules.asPolicy())

        val result = provisioner.diff(acl)
        assertThat(result.result?.missing).isTrue

        provisioner.apply(acl)

        val afterApplyResult = provisioner.diff(acl)
        assertThat(afterApplyResult.result?.missing).isFalse

        // update is always enforced to ensure rules are up-to-date
        assertThat(provisioner.diff(acl).result?.hasChanges()).isTrue
    }
}
