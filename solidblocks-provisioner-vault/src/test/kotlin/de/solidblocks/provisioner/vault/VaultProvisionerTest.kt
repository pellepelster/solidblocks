package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.SolidblocksDatabase
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
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import de.solidblocks.vault.VaultRootClientProvider
import junit.framework.Assert.assertFalse
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
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
@ExtendWith(SolidblocksTestDatabaseExtension::class)
class VaultProvisionerTest {

    companion object {

        private var vaultClient: VaultTemplate? = null

        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withPull(true)
                    withExposedService("vault", 8200)
                    start()
                }

        private fun vaultAddress() = "http://localhost:${environment.getServicePort("vault", 8200)}"

        fun vaultTemplateProvider(solidblocksDatabase: SolidblocksDatabase): () -> VaultTemplate {

            if (vaultClient == null) {
                val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
                val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

                val cloudName = UUID.randomUUID().toString()
                val environmentName = UUID.randomUUID().toString()

                cloudRepository.createCloud(cloudName, "domain1", emptyList())
                environmentRepository.createEnvironment(cloudName, environmentName)

                vaultClient = VaultRootClientProvider(
                    cloudName,
                    environmentName,
                    environmentRepository,
                    vaultAddress()
                ).createClient()
            }

            return { vaultClient!! }
        }
    }

    @Test
    fun testMountDiffAndApply(solidblocksDatabase: SolidblocksDatabase) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(solidblocksDatabase))
        val kvProvisioner = VaultKVProvisioner(vaultTemplateProvider(solidblocksDatabase))

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
    fun testPkiBackendRoleDiffAndApply(solidblocksDatabase: SolidblocksDatabase) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(solidblocksDatabase))
        val roleProvisioner = VaultPkiBackendRoleProvisioner(vaultTemplateProvider(solidblocksDatabase))

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
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)

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
        assertTrue(updateDiff.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testDiffAndApplySsh(solidblocksDatabase: SolidblocksDatabase) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(solidblocksDatabase))

        val mount = VaultMount("${UUID.randomUUID()}-ssh", "ssh")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testDiffAndApplyKv2(solidblocksDatabase: SolidblocksDatabase) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(solidblocksDatabase))

        val mount = VaultMount("${UUID.randomUUID()}-ssh", "kv-v2")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testDiffAndApplyPki(solidblocksDatabase: SolidblocksDatabase) {

        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(solidblocksDatabase))

        val mount = VaultMount("${UUID.randomUUID()}-pki", "pki")

        val diffBefore = mountProvisioner.diff(mount)
        assertTrue(diffBefore.result?.hasChangesOrMissing()!!)

        mountProvisioner.apply(mount)

        val diffAfter = mountProvisioner.diff(mount)
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testSSHBackendRoleDiffAndApply(solidblocksDatabase: SolidblocksDatabase) {
        val mountProvisioner = VaultMountProvisioner(vaultTemplateProvider(solidblocksDatabase))
        val sshBackendRoleProvisioner = VaultSshBackendRoleProvisioner(vaultTemplateProvider(solidblocksDatabase))

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
        assertFalse(diffAfter.result?.hasChangesOrMissing()!!)

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
        assertTrue(updateDiff.result?.hasChangesOrMissing()!!)
    }

    @Test
    fun testPolicyDiffAndApply(solidblocksDatabase: SolidblocksDatabase) {

        val policyProvisioner = VaultPolicyProvisioner(vaultTemplateProvider(solidblocksDatabase))

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
