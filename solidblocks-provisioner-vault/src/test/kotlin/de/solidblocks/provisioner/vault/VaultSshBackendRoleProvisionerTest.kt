package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.mount.VaultMountProvisioner
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRoleProvisioner
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
class VaultSshBackendRoleProvisionerTest(
    @Autowired
    val provisioner: Provisioner,

    @Autowired
    val sshBackendRoleProvisioner: VaultSshBackendRoleProvisioner,

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
        val mount = VaultMount(vaultTestFixture.cloudName, "ssh")
        mountProvisioner.apply(mount)

        val newRole = VaultSshBackendRole(
            name = "solidblocks-host-ssh",
            keyType = "ca",
            maxTtl = "168h",
            ttl = "168h",
            allowHostCertificates = true,
            allowUserCertificates = false,
            mount = mount
        )

        // initially the role is missing
        val diffBefore = sshBackendRoleProvisioner.diff(newRole)
        assertTrue(diffBefore.result?.missing!!)

        // apply should go through
        val applyResult = sshBackendRoleProvisioner.apply(newRole)
        assertFalse(applyResult.failed)

        // no changes after apply
        val diffAfter = sshBackendRoleProvisioner.diff(newRole)
        assertFalse(diffAfter.result?.hasChanges()!!)

        val lookup = sshBackendRoleProvisioner.lookup(newRole)

        assertThat(lookup.result?.backendRole?.allow_host_certificates, Is.`is`(true))
        assertThat(lookup.result?.backendRole?.allow_user_certificates, Is.`is`(false))
        assertThat(lookup.result?.backendRole?.ttl, Is.`is`("604800"))
        assertThat(lookup.result?.backendRole?.max_ttl, Is.`is`("604800"))
        assertThat(lookup.result?.backendRole?.key_type, Is.`is`("ca"))
        assertTrue(lookup.result?.keysExist!!)

        val updateRole = VaultSshBackendRole(
            name = "solidblocks-host-ssh",
            keyType = "ca",
            maxTtl = "170h",
            ttl = "170h",
            allowHostCertificates = true,
            allowUserCertificates = false,
            mount = mount
        )

        // changes due to updated ttl's
        val updateDiff = sshBackendRoleProvisioner.diff(updateRole)
        assertTrue(updateDiff.result?.hasChanges()!!)
    }
}
