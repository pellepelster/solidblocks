package de.solidblocks.config

import de.solidblocks.cloud.config.CloudConfigValue
import de.solidblocks.cloud.config.CloudConfigurationManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [TestConfiguration::class]
)
@AutoConfigureTestDatabase
class CloudConfigurationManagerTest {

    @Autowired
    private lateinit var cloudConfigurationManager: CloudConfigurationManager

    @Test
    fun testCreateCloud() {
        assertThat(cloudConfigurationManager.hasCloud("cloud1")).isFalse

        assertThat(cloudConfigurationManager.createCloud("cloud1", "domain1", listOf(CloudConfigValue("name1", "value1")))).isTrue
        assertThat(cloudConfigurationManager.hasCloud("cloud1")).isTrue
        assertThat(cloudConfigurationManager.createCloud("cloud1", "domain1")).isFalse

        val cloudConfigWithoutEnv = cloudConfigurationManager.cloudByName("cloud1")
        assertThat(cloudConfigWithoutEnv.name).isEqualTo("cloud1")
        assertThat(cloudConfigWithoutEnv.rootDomain).isEqualTo("domain1")
        assertThat(cloudConfigWithoutEnv.environments).hasSize(0)

        cloudConfigurationManager.createEnvironment("cloud1", "env1")

        val cloudConfig = cloudConfigurationManager.cloudByName("cloud1")
        assertThat(cloudConfig.environments).hasSize(1)
        assertThat(cloudConfig.environments[0].sshSecrets.sshIdentityPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(cloudConfig.environments[0].sshSecrets.sshIdentityPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(cloudConfig.environments[0].sshSecrets.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(cloudConfig.environments[0].sshSecrets.sshPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(cloudConfig.configValues).anyMatch { it.name == "name1" && it.value == "value1" }
    }


    @Test
    fun testUpdateEnvironment() {
        cloudConfigurationManager.createCloud("cloud3", "domain1")
        cloudConfigurationManager.createEnvironment("cloud3", "env3")

        val cloudConfig = cloudConfigurationManager.cloudByName("cloud3")
        assertThat(cloudConfig.environments[0].configValues).filteredOn { it.name == "my-attribute" }.hasSize(0)

        cloudConfigurationManager.updateEnvironment("cloud3", "env3", "my-attribute", "my-value")
        val newCloudConfig = cloudConfigurationManager.cloudByName("cloud3")

        assertThat(newCloudConfig.environments[0].configValues).filteredOn { it.name == "my-attribute" }.hasSize(1)
        assertThat(newCloudConfig.environments[0].configValues).anyMatch { it.name == "my-attribute" && it.value == "my-value" }
    }

    @Test
    fun testRegenerateCloudSecrets() {
        assertThat(cloudConfigurationManager.createCloud("cloud2", "domain2")).isTrue
        cloudConfigurationManager.createEnvironment("cloud2", "env2")

        val cloudConfig = cloudConfigurationManager.cloudByName("cloud2")
        assertThat(cloudConfig.environments).hasSize(1)
        assertThat(cloudConfig.environments[0].sshSecrets.sshIdentityPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(cloudConfig.environments[0].sshSecrets.sshIdentityPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(cloudConfig.environments[0].sshSecrets.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(cloudConfig.environments[0].sshSecrets.sshPublicKey).startsWith("ssh-ed25519 AAAA")

        cloudConfigurationManager.rotateEnvironmentSecrets("cloud2", "env2")

        val newCloudConfig = cloudConfigurationManager.cloudByName("cloud2")
        assertThat(newCloudConfig.environments).hasSize(1)
        assertThat(newCloudConfig.environments[0].sshSecrets.sshIdentityPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(newCloudConfig.environments[0].sshSecrets.sshIdentityPublicKey).startsWith("ssh-ed25519 AAAA")
        assertThat(newCloudConfig.environments[0].sshSecrets.sshPrivateKey).startsWith("-----BEGIN OPENSSH PRIVATE KEY-----")
        assertThat(newCloudConfig.environments[0].sshSecrets.sshPublicKey).startsWith("ssh-ed25519 AAAA")

        assertThat(newCloudConfig.environments[0].sshSecrets.sshIdentityPrivateKey).isNotEqualTo(cloudConfig.environments[0].sshSecrets.sshIdentityPrivateKey)
        assertThat(newCloudConfig.environments[0].sshSecrets.sshIdentityPublicKey).isNotEqualTo(cloudConfig.environments[0].sshSecrets.sshIdentityPublicKey)
        assertThat(newCloudConfig.environments[0].sshSecrets.sshPrivateKey).isNotEqualTo(cloudConfig.environments[0].sshSecrets.sshPrivateKey)
        assertThat(newCloudConfig.environments[0].sshSecrets.sshPublicKey).isNotEqualTo(cloudConfig.environments[0].sshSecrets.sshPublicKey)
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
