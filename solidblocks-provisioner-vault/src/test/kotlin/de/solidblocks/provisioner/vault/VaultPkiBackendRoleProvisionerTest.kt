package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.mount.VaultMountProvisioner
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRoleProvisioner
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
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
class VaultPkiBackendRoleProvisionerTest(
    @Autowired
    val provisioner: Provisioner,

    @Autowired
    val roleProvisioner: VaultPkiBackendRoleProvisioner,

    @Autowired
    val mountProvisioner: VaultMountProvisioner,

    @Autowired
    val cloudConfigurationManager: CloudConfigurationManager
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
        val mount = VaultMount(vaultTestFixture.cloudName, "pki")
        mountProvisioner.apply(mount)

        val newRole = VaultPkiBackendRole(
            name = "solidblocks-pki",
            allowAnyName = true,
            generateLease = true,
            maxTtl = "168h",
            ttl = "168h",
            keyBits = 521,
            keyType = "ec",
            mount = mount
        )

        // initially the role is missing
        val diffBefore = roleProvisioner.diff(newRole)
        assertTrue(diffBefore.result?.missing!!)

        // apply should go through
        val applyResult = roleProvisioner.apply(newRole)
        assertFalse(applyResult.failed)

        // no changes after apply
        val diffAfter = roleProvisioner.diff(newRole)
        assertFalse(diffAfter.result?.hasChanges()!!)

        val lookup = roleProvisioner.lookup(newRole)

        assertThat(lookup.result?.backendRole?.allow_any_name, Is.`is`(true))
        assertThat(lookup.result?.backendRole?.generate_lease, Is.`is`(true))
        assertThat(lookup.result?.backendRole?.ttl, Is.`is`("604800"))
        assertThat(lookup.result?.backendRole?.max_ttl, Is.`is`("604800"))
        assertThat(lookup.result?.backendRole?.key_type, Is.`is`("ec"))
        assertThat(lookup.result?.backendRole?.key_bits, Is.`is`(521))
        assertTrue(lookup.result?.keysExist!!)

        val updateRole = VaultPkiBackendRole(
            name = "solidblocks-pki",
            allowAnyName = true,
            generateLease = true,
            maxTtl = "170h",
            ttl = "170h",
            keyBits = 521,
            keyType = "ec",
            mount = mount
        )

        // changes due to updated ttl's
        val updateDiff = roleProvisioner.diff(updateRole)
        assertTrue(updateDiff.result?.hasChanges()!!)
    }
}
