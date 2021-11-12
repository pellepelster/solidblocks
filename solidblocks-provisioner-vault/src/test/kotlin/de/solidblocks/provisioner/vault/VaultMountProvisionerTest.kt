package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.mount.VaultMountProvisioner
import org.junit.ClassRule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.DockerComposeContainer
import java.io.File

@SpringBootTest(classes = [TestConfiguration::class])
@AutoConfigureTestDatabase
class VaultMountProvisionerTest(

    @Autowired
    val provisioner: Provisioner,

    @Autowired
    val cloudConfigurationManager: CloudConfigurationManager,

    @Autowired
    val mountProvisioner: VaultMountProvisioner,
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
    fun testDiffAndApplySsh() {
        val mount = VaultMount("${vaultTestFixture.cloudName}-ssh", "ssh")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChanges()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChanges()!!)
    }

    @Test
    fun testDiffAndApplyKv2() {
        val mount = VaultMount("${vaultTestFixture.cloudName}-ssh", "kv-v2")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChanges()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChanges()!!)
    }

    @Test
    fun testDiffAndApplyPki() {
        val mount = VaultMount("${vaultTestFixture.cloudName}-pki", "pki")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChanges()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChanges()!!)
    }
}
