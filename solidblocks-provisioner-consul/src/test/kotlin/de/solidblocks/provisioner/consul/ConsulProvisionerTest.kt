package de.solidblocks.provisioner.consul

import de.solidblocks.provisioner.consul.acl.ConsulAcl
import de.solidblocks.provisioner.consul.acl.ConsulAclProvisioner
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.DockerComposeContainer
import java.io.File

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@SpringBootTest(classes = [TestConfiguration::class], properties = ["spring.main.allow-bean-definition-overriding=true"])
class ConsulProvisionerTest {

    @Autowired
    private lateinit var provisioner: ConsulAclProvisioner

    @ClassRule
    var environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                    .apply {
                        withExposedService("consul", 8500)
                        start()
                    }


    @Test
    fun testDiffAndApply() {
        val acl = ConsulAcl("acl")
        val result = provisioner.diff(acl)
        assertThat(result.result?.missing).isTrue

        provisioner.apply(acl)

        val afterApplyResult = provisioner.diff(acl)
        assertThat(afterApplyResult.result?.missing).isFalse

    }
}
