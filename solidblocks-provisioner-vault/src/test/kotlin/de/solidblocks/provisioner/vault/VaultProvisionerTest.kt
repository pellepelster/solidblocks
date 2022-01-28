package de.solidblocks.provisioner.vault

import de.solidblocks.base.reference.EnvironmentReference
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
import de.solidblocks.test.TestEnvironment
import de.solidblocks.test.TestEnvironmentExtension
import junit.framework.Assert.assertFalse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.support.Policy
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

@Testcontainers
@ExtendWith(TestEnvironmentExtension::class)
class VaultProvisionerTest {

    companion object {

        private var vaultClient: VaultTemplate? = null

        lateinit var reference: EnvironmentReference

        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withExposedService("vault", 8200)
                    start()
                }

        private fun vaultAddress() = "http://localhost:${environment.getServicePort("vault", 8200)}"

        fun vaultTemplateProvider(testEnvironment: TestEnvironment): () -> VaultTemplate {
            if (vaultClient == null) {
                reference = testEnvironment.createCloudAndEnvironment(UUID.randomUUID().toString())

                vaultClient = VaultRootClientProvider(
                    reference,
                    testEnvironment.repositories.environments,
                    vaultAddress()
                ).createClient()
            }

            return { vaultClient!! }
        }
    }

    @Test
    fun testMountDiffAndApply(testEnvironment: TestEnvironment) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(testEnvironment))
        val kvProvisioner = VaultKVProvisioner(vaultTemplateProvider(testEnvironment))

        val mount = VaultMount(UUID.randomUUID().toString(), "kv-v2")
        val result = mountProvisioner.apply(mount)
        assertFalse(result.failed)

        val kv = VaultKV("pelle", mapOf("aa" to "bb"), mount)

        val diffBefore = kvProvisioner.diff(kv)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        kvProvisioner.apply(kv)

        val diffAfter = kvProvisioner.diff(kv)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)

        val kvNew = VaultKV("pelle", mapOf("aa" to "cc"), mount)
        val diffAfterNew = kvProvisioner.diff(kvNew)
        assertTrue(diffAfterNew.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testPkiBackendRoleDiffAndApply(testEnvironment: TestEnvironment) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(testEnvironment))
        val roleProvisioner = VaultPkiBackendRoleProvisioner(vaultTemplateProvider(testEnvironment))

        val mount = VaultMount(UUID.randomUUID().toString(), "pki")
        mountProvisioner.apply(mount)

        val newRole = VaultPkiBackendRole(
            name = "solidblocks-pki",
            allowedDomains = listOf("test.org"),
            allowSubdomains = true,
            allowLocalhost = false,
            generateLease = true,
            serverFlag = true,
            clientFlag = false,
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
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)

        val lookup = roleProvisioner.lookup(newRole)

        assertThat(lookup.result?.backendRole?.allowed_domains).isEqualTo(listOf("test.org"))
        assertThat(lookup.result?.backendRole?.allow_subdomains).isEqualTo(true)
        assertThat(lookup.result?.backendRole?.allow_localhost).isEqualTo(false)
        assertThat(lookup.result?.backendRole?.generate_lease).isEqualTo(true)
        assertThat(lookup.result?.backendRole?.ttl).isEqualTo("604800")
        assertThat(lookup.result?.backendRole?.max_ttl).isEqualTo("604800")
        assertThat(lookup.result?.backendRole?.key_type).isEqualTo("ec")
        assertThat(lookup.result?.backendRole?.key_bits).isEqualTo(521)
        assertTrue(lookup.result?.keysExist!!)

        val updateRole = VaultPkiBackendRole(
            name = "solidblocks-pki",
            allowedDomains = listOf("test.org"),
            allowSubdomains = true,
            allowLocalhost = false,
            generateLease = true,
            serverFlag = true,
            clientFlag = false,
            maxTtl = "170h",
            ttl = "170h",
            keyBits = 521,
            keyType = "ec",
            mount = mount
        )

        // changes due to updated ttl's
        val updateDiff = roleProvisioner.diff(updateRole)
        assertTrue(updateDiff.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testDiffAndApplySsh(testEnvironment: TestEnvironment) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(testEnvironment))

        val mount = VaultMount("${UUID.randomUUID()}-ssh", "ssh")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testDiffAndApplyKv2(testEnvironment: TestEnvironment) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(testEnvironment))

        val mount = VaultMount("${UUID.randomUUID()}-ssh", "kv-v2")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testDiffAndApplyPki(testEnvironment: TestEnvironment) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(testEnvironment))

        val mount = VaultMount("${UUID.randomUUID()}-pki", "pki")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testSSHBackendRoleDiffAndApply(testEnvironment: TestEnvironment) {
        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(testEnvironment))
        val sshBackendRoleProvisioner = VaultSshBackendRoleProvisioner(vaultTemplateProvider(testEnvironment))

        val mount = VaultMount(UUID.randomUUID().toString(), "ssh")
        mountProvisioner.apply(mount)

        val newRole = VaultSshBackendRole(
            name = "solidblocks-ssh-host",
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
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)

        val lookup = sshBackendRoleProvisioner.lookup(newRole)

        assertThat(lookup.result?.backendRole?.allow_host_certificates).isEqualTo(true)
        assertThat(lookup.result?.backendRole?.allow_user_certificates).isEqualTo(false)
        assertThat(lookup.result?.backendRole?.ttl).isEqualTo("604800")
        assertThat(lookup.result?.backendRole?.max_ttl).isEqualTo("604800")
        assertThat(lookup.result?.backendRole?.key_type).isEqualTo("ca")
        assertTrue(lookup.result?.keysExist!!)

        val updateRole = VaultSshBackendRole(
            name = "solidblocks-ssh-host",
            keyType = "ca",
            maxTtl = "170h",
            ttl = "170h",
            allowHostCertificates = true,
            allowUserCertificates = false,
            mount = mount
        )

        // changes due to updated ttl's
        val updateDiff = sshBackendRoleProvisioner.diff(updateRole)
        assertTrue(updateDiff.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testPolicyDiffAndApply(testEnvironment: TestEnvironment) {

        val policyProvisioner = VaultPolicyProvisioner(vaultTemplateProvider(testEnvironment))

        val name = UUID.randomUUID().toString()
        val emptyPolicy = VaultPolicy(name, emptySet())

        val diffBefore = policyProvisioner.diff(emptyPolicy)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        policyProvisioner.apply(emptyPolicy)

        val diffAfter = policyProvisioner.diff(emptyPolicy)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)

        val policyWithRules = VaultPolicy(
            name,
            setOf(Policy.Rule.builder().path("mypath").capability(Policy.BuiltinCapabilities.READ).build())
        )
        val diffBeforeWithRules = policyProvisioner.diff(policyWithRules)
        assertTrue(diffBeforeWithRules.result?.hasChangesOrMissing()!!)

        policyProvisioner.apply(policyWithRules)

        val diffAfterWithRules = policyProvisioner.diff(policyWithRules)
        assertFalse(diffAfterWithRules.result?.hasChangesOrMissing()!!)
    }
}
