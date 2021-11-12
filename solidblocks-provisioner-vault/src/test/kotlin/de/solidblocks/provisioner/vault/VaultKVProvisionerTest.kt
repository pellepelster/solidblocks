package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.kv.VaultKVProvisioner
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
open class VaultKVProvisionerTest(

    @Autowired
    val provisioner: Provisioner,

    @Autowired
    val cloudConfigurationManager: CloudConfigurationManager,

    @Autowired
    val mountProvisioner: VaultMountProvisioner,

    @Autowired
    val kvProvisioner: VaultKVProvisioner,
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
        val mount = VaultMount(vaultTestFixture.cloudName, "kv-v2")
        val result = mountProvisioner.apply(mount)
        assertFalse(result.failed)

        val kv = VaultKV("pelle", mapOf("aa" to "bb"), mount)

        val diffBefore = kvProvisioner.diff(kv)
        assertTrue(diffBefore.result?.hasChanges()!!)

        kvProvisioner.apply(kv)

        val diffAfter = kvProvisioner.diff(kv)
        assertFalse(diffAfter.result?.hasChanges()!!)

        val kvNew = VaultKV("pelle", mapOf("aa" to "cc"), mount)
        val diffAfterNew = kvProvisioner.diff(kvNew)
        assertTrue(diffAfterNew.result?.hasChanges()!!)
    }
}
