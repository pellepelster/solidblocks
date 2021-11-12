package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.policy.VaultPolicyProvisioner
import org.junit.ClassRule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.vault.support.Policy.BuiltinCapabilities
import org.springframework.vault.support.Policy.Rule
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.*

@SpringBootTest(classes = [TestConfiguration::class])
@AutoConfigureTestDatabase
class VaultPolicyProvisionerTest(

    @Autowired
    val provisioner: Provisioner,

    @Autowired
    val cloudConfigurationManager: CloudConfigurationManager,

    @Autowired
    val policyProvisioner: VaultPolicyProvisioner
) {

    @ClassRule
    var environment: DockerComposeContainer<*> =
        KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
            .apply {
                withExposedService("vault1", 8200)
                withExposedService("vault2", 8200)
                start()
            }

    val vaultTestFixture = VaultTestFixture(environment, cloudConfigurationManager, provisioner)

    @Test
    fun testDiffAndApply() {
        val emptyPolicy = VaultPolicy(vaultTestFixture.cloudName, emptySet())

        val diffBefore = policyProvisioner.diff(emptyPolicy)
        assertTrue(diffBefore.result?.hasChanges()!!)

        policyProvisioner.apply(emptyPolicy)

        val diffAfter = policyProvisioner.diff(emptyPolicy)
        assertFalse(diffAfter.result?.hasChanges()!!)

        val policyWithRules = VaultPolicy(
            vaultTestFixture.cloudName,
            setOf(Rule.builder().path("mypath").capability(BuiltinCapabilities.READ).build())
        )
        val diffBeforeWithRules = policyProvisioner.diff(policyWithRules)
        assertTrue(diffBeforeWithRules.result?.hasChanges()!!)

        policyProvisioner.apply(policyWithRules)

        val diffAfterWithRules = policyProvisioner.diff(policyWithRules)
        assertFalse(diffAfterWithRules.result?.hasChanges()!!)
    }
}
