package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import org.junit.ClassRule
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.DockerComposeContainer
import java.io.File
import java.util.*

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@SpringBootTest(classes = [TestApplicationContext::class])
@AutoConfigureTestDatabase
class VaultRootClientProviderTest(
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

    @Test
    fun testInitAndUnseal() {

        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()
        cloudConfigurationManager.createCloud(cloudName, "domain1")
        cloudConfigurationManager.createEnvironment(cloudName, environmentName)

        val context = CloudConfigurationContext(cloudConfigurationManager.cloudByName(cloudName), cloudConfigurationManager.environmentByName(cloudName, environmentName))
        val provider =
                VaultRootClientProvider(
                        context,
                        cloudConfigurationManager,
                        "http://localhost:8200"
                )

        val vaultClient = provider.createClient()

        val environment = cloudConfigurationManager.environmentByName(cloudName, environmentName)

        assertTrue(environment.configValues.any { it.name == "vault-unseal-key-0" })
        assertTrue(environment.configValues.any { it.name == "vault-unseal-key-1" })
        assertTrue(environment.configValues.any { it.name == "vault-unseal-key-2" })
        assertTrue(environment.configValues.any { it.name == "vault-unseal-key-3" })
        assertTrue(environment.configValues.any { it.name == "vault-unseal-key-4" })
        assertTrue(environment.configValues.any { it.name == "vault-root-token" })

        assertFalse(vaultClient.opsForSys().unsealStatus.isSealed)
        assertTrue(vaultClient.opsForSys().isInitialized)

        val vaultClient1 = provider.createClient()
        assertFalse(vaultClient1.opsForSys().unsealStatus.isSealed)
    }
}
