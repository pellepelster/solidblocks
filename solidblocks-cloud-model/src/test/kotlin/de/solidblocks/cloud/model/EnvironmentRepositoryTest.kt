package de.solidblocks.cloud.model

import de.solidblocks.test.SolidblocksTestDatabaseExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTestDatabaseExtension::class)
class EnvironmentRepositoryTest {

    @Test
    fun testCreateEnvironment(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

        val cloud = cloudRepository.createCloud("cloud1", "domain1")

        assertThat(environmentRepository.createEnvironment(cloud.name, "env1")).isNotNull

        val environment = environmentRepository.getEnvironment("cloud1", "env1")
        assertThat(environment.sshSecrets.sshIdentityPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(environment.sshSecrets.sshIdentityPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(environment.sshSecrets.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(environment.sshSecrets.sshPublicKey).startsWith("ssh-ed25519 AAAA")
    }

    @Test
    fun testCreateAndUpdateEnvironment(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

        val cloud = cloudRepository.createCloud("cloud3", "domain1")

        assertThat(environmentRepository.createEnvironment(cloud.name, "env3")).isNotNull

        val environment = environmentRepository.getEnvironment("cloud3", "env3")
        assertThat(environment.configValues).filteredOn { it.name == "my-attribute" }.hasSize(0)

        environmentRepository.updateEnvironment("cloud3", "env3", "my-attribute", "my-value")
        val updatedEnvironment = environmentRepository.getEnvironment("cloud3", "env3")

        assertThat(updatedEnvironment.configValues).filteredOn { it.name == "my-attribute" }.hasSize(1)
        assertThat(updatedEnvironment.configValues).anyMatch { it.name == "my-attribute" && it.value == "my-value" }
    }

    @Test
    fun testRegenerateCloudSecrets(solidblocksDatabase: SolidblocksDatabase) {
        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

        val cloud = cloudRepository.createCloud("cloud4", "domain4")

        assertThat(environmentRepository.createEnvironment(cloud.name, "env4")).isNotNull

        val newEnv2 = environmentRepository.getEnvironment("cloud4", "env4")
        assertThat(newEnv2.sshSecrets.sshIdentityPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(newEnv2.sshSecrets.sshIdentityPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(newEnv2.sshSecrets.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(newEnv2.sshSecrets.sshPublicKey).startsWith("ssh-ed25519 AAAA")

        environmentRepository.rotateEnvironmentSecrets("cloud4", "env4")

        val updatedEnv2 = environmentRepository.getEnvironment("cloud4", "env4")
        assertThat(updatedEnv2.sshSecrets.sshIdentityPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(updatedEnv2.sshSecrets.sshIdentityPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(updatedEnv2.sshSecrets.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(updatedEnv2.sshSecrets.sshPublicKey).startsWith("ssh-ed25519 AAAA")

        assertThat(updatedEnv2.sshSecrets.sshIdentityPrivateKey).isNotEqualTo(newEnv2.sshSecrets.sshIdentityPrivateKey)
        assertThat(updatedEnv2.sshSecrets.sshIdentityPublicKey).isNotEqualTo(newEnv2.sshSecrets.sshIdentityPublicKey)
        assertThat(updatedEnv2.sshSecrets.sshPrivateKey).isNotEqualTo(newEnv2.sshSecrets.sshPrivateKey)
        assertThat(updatedEnv2.sshSecrets.sshPublicKey).isNotEqualTo(newEnv2.sshSecrets.sshPublicKey)
    }

    /*
    @Test
    fun testList() {
        val oldResult = cloudConfigurationManager.list()

        val name = UUID.randomUUID().toString()
        cloudConfigurationManager.create(name, "domain1", "email1", emptyList())

        val newResult = cloudConfigurationManager.list()

        assertThat(newResult.size, Is.`is`(oldResult.size + 1))
    }

    @Test
    fun testCreate() {
        val name = UUID.randomUUID().toString()
        cloudConfigurationManager.create(name, "domain1", "email1", listOf(CloudConfigValue("hetzner_cloud", "abc")))

        val cloud = cloudConfigurationManager.getTenant(name)

        assertThat(cloud.solidblocksConfig.domain, Is.`is`("domain1"))
        assertThat(cloud.name, Is.`is`(name))

        assertThat(cloud.sshConfig.sshIdentityPrivateKey, containsString("BEGIN OPENSSH PRIVATE KEY"))
        assertThat(cloud.sshConfig.sshIdentityPublicKey, containsString("ssh-ed25519 AAAA"))
        assertThat(cloud.sshConfig.sshPrivateKey, containsString("BEGIN OPENSSH PRIVATE KEY"))
        assertThat(cloud.sshConfig.sshPublicKey, containsString("ssh-ed25519 AAAA"))
        assertThat(
            cloud.configurations.firstOrNull { it.name == CONFIG_SSH_IDENTITY_PUBLIC_KEY }?.version,
            Is.`is`(0)
        )
        assertThat(
            cloud.configurations.firstOrNull { it.name == CONFIG_SSH_IDENTITY_PRIVATE_KEY }?.version,
            Is.`is`(0)
        )
    }

    @Test
    fun testDelete() {
        val name = UUID.randomUUID().toString()
        cloudConfigurationManager.create(name, "domain1", "email1", emptyList())
        assertTrue(cloudConfigurationManager.hasTenant(name))

        cloudConfigurationManager.delete(name)
        assertFalse(cloudConfigurationManager.hasTenant(name))
    }

    @Test
    fun testRegenerateSecrets() {
        val name = UUID.randomUUID().toString()
        cloudConfigurationManager.create(name, "domain1", "email1", emptyList())

        val beforeRegenerate = cloudConfigurationManager.getTenant(name)
        assertThat(beforeRegenerate.name, Is.`is`(name))
        assertThat(
            beforeRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_IDENTITY_PUBLIC_KEY }?.version,
            Is.`is`(0)
        )
        assertThat(
            beforeRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_IDENTITY_PRIVATE_KEY }?.version,
            Is.`is`(0)
        )
        assertThat(
            beforeRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_PUBLIC_KEY }?.version,
            Is.`is`(0)
        )
        assertThat(
            beforeRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_PRIVATE_KEY }?.version,
            Is.`is`(0)
        )

        cloudConfigurationManager.regenerateSecrets(name)
        val afterRegenerate = cloudConfigurationManager.getTenant(name)

        assertThat(afterRegenerate.name, Is.`is`(name))

        assertThat(afterRegenerate.sshConfig.sshIdentityPrivateKey, containsString("BEGIN OPENSSH PRIVATE KEY"))
        assertThat(afterRegenerate.sshConfig.sshIdentityPublicKey, containsString("ssh-ed25519 AAAA"))
        assertThat(
            afterRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_IDENTITY_PUBLIC_KEY }?.version,
            Is.`is`(1)
        )
        assertThat(
            afterRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_IDENTITY_PRIVATE_KEY }?.version,
            Is.`is`(1)
        )
        assertThat(afterRegenerate.sshConfig.sshPrivateKey, containsString("BEGIN OPENSSH PRIVATE KEY"))
        assertThat(afterRegenerate.sshConfig.sshPublicKey, containsString("ssh-ed25519 AAAA"))
        assertThat(
            afterRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_PUBLIC_KEY }?.version,
            Is.`is`(1)
        )
        assertThat(
            afterRegenerate.configurations.firstOrNull { it.name == CONFIG_SSH_PRIVATE_KEY }?.version,
            Is.`is`(1)
        )

        assertThat(
            beforeRegenerate.sshConfig.sshIdentityPrivateKey,
            IsNot.not(
                afterRegenerate.sshConfig.sshIdentityPrivateKey
            )
        )
        assertThat(
            beforeRegenerate.sshConfig.sshIdentityPublicKey,
            IsNot.not(afterRegenerate.sshConfig.sshIdentityPublicKey)
        )
        assertThat(
            beforeRegenerate.sshConfig.sshPrivateKey,
            IsNot.not(
                afterRegenerate.sshConfig.sshPrivateKey
            )
        )
        assertThat(
            beforeRegenerate.sshConfig.sshPublicKey,
            IsNot.not(afterRegenerate.sshConfig.sshPublicKey)
        )
    }*/
}
