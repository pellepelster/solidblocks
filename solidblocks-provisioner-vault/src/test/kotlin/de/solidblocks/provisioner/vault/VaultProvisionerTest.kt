package de.solidblocks.provisioner.vault

import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.kv.VaultKVProvisioner
import de.solidblocks.provisioner.vault.mount.VaultMount
import de.solidblocks.provisioner.vault.mount.VaultMountProvisioner
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRole
import de.solidblocks.provisioner.vault.pki.VaultPkiBackendRoleProvisioner
import de.solidblocks.provisioner.vault.policy.VaultPolicy
import de.solidblocks.provisioner.vault.policy.VaultPolicyProvisioner
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRole
import de.solidblocks.provisioner.vault.ssh.VaultSshBackendRoleProvisioner
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.vault.support.Policy
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

@SpringBootTest(classes = [TestApplicationContext::class], properties = ["spring.main.allow-bean-definition-overriding=true"])
@AutoConfigureTestDatabase
@TestPropertySource(properties = ["vault_addr=http://localhost:8200"])
@Testcontainers
open class VaultProvisionerTest(
        @Autowired
        val mountProvisioner: VaultMountProvisioner,

        @Autowired
        val roleProvisioner: VaultPkiBackendRoleProvisioner,

        @Autowired
        val sshBackendRoleProvisioner: VaultSshBackendRoleProvisioner,

        @Autowired
        val policyProvisioner: VaultPolicyProvisioner,

        @Autowired
        val kvProvisioner: VaultKVProvisioner,
) {

    @Container
    private final var environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                    .apply {
                        withExposedService("vault1", 8200)
                        withExposedService("vault2", 8200)
                        start()
                    }

    @Test
    fun testMountDiffAndApply() {
        val mount = VaultMount(UUID.randomUUID().toString(), "kv-v2")
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

    @Test
    fun testPkiBackendRoleDiffAndApply() {
        val mount = VaultMount(UUID.randomUUID().toString(), "pki")
        mountProvisioner.apply(mount)

        val newRole = VaultPkiBackendRole(
                id = "solidblocks-pki",
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

        MatcherAssert.assertThat(lookup.result?.backendRole?.allow_any_name, Is.`is`(true))
        MatcherAssert.assertThat(lookup.result?.backendRole?.generate_lease, Is.`is`(true))
        MatcherAssert.assertThat(lookup.result?.backendRole?.ttl, Is.`is`("604800"))
        MatcherAssert.assertThat(lookup.result?.backendRole?.max_ttl, Is.`is`("604800"))
        MatcherAssert.assertThat(lookup.result?.backendRole?.key_type, Is.`is`("ec"))
        MatcherAssert.assertThat(lookup.result?.backendRole?.key_bits, Is.`is`(521))
        assertTrue(lookup.result?.keysExist!!)

        val updateRole = VaultPkiBackendRole(
                id = "solidblocks-pki",
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


    @Test
    fun testDiffAndApplySsh() {
        val mount = VaultMount("${UUID.randomUUID()}-ssh", "ssh")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChanges()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChanges()!!)
    }

    @Test
    fun testDiffAndApplyKv2() {
        val mount = VaultMount("${UUID.randomUUID()}-ssh", "kv-v2")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChanges()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChanges()!!)
    }

    @Test
    fun testDiffAndApplyPki() {
        val mount = VaultMount("${UUID.randomUUID()}-pki", "pki")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChanges()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChanges()!!)
    }

    @Test
    fun testSSHBackendRoleDiffAndApply() {
        val mount = VaultMount(UUID.randomUUID().toString(), "ssh")
        mountProvisioner.apply(mount)

        val newRole = VaultSshBackendRole(
                id = "solidblocks-host-ssh",
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

        MatcherAssert.assertThat(lookup.result?.backendRole?.allow_host_certificates, Is.`is`(true))
        MatcherAssert.assertThat(lookup.result?.backendRole?.allow_user_certificates, Is.`is`(false))
        MatcherAssert.assertThat(lookup.result?.backendRole?.ttl, Is.`is`("604800"))
        MatcherAssert.assertThat(lookup.result?.backendRole?.max_ttl, Is.`is`("604800"))
        MatcherAssert.assertThat(lookup.result?.backendRole?.key_type, Is.`is`("ca"))
        assertTrue(lookup.result?.keysExist!!)

        val updateRole = VaultSshBackendRole(
                id = "solidblocks-host-ssh",
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

    @Test
    fun testPolicyDiffAndApply() {
        val name = UUID.randomUUID().toString()
        val emptyPolicy = VaultPolicy(name, emptySet())

        val diffBefore = policyProvisioner.diff(emptyPolicy)
        assertTrue(diffBefore.result?.hasChanges()!!)

        policyProvisioner.apply(emptyPolicy)

        val diffAfter = policyProvisioner.diff(emptyPolicy)
        assertFalse(diffAfter.result?.hasChanges()!!)

        val policyWithRules = VaultPolicy(
                name,
                setOf(Policy.Rule.builder().path("mypath").capability(Policy.BuiltinCapabilities.READ).build())
        )
        val diffBeforeWithRules = policyProvisioner.diff(policyWithRules)
        assertTrue(diffBeforeWithRules.result?.hasChanges()!!)

        policyProvisioner.apply(policyWithRules)

        val diffAfterWithRules = policyProvisioner.diff(policyWithRules)
        assertFalse(diffAfterWithRules.result?.hasChanges()!!)
    }


}
