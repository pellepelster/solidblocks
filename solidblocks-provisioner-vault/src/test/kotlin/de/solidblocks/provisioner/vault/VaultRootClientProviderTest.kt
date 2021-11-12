package de.solidblocks.provisioner.vault

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

@SpringBootTest(classes = [TestConfiguration::class])
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
        cloudConfigurationManager.create(cloudName, "domain1", "email1", emptyList())

        val provider =
            VaultRootClientProvider(
                cloudName,
                "http://localhost:${environment.getServicePort("vault1", 8200)}",
                cloudConfigurationManager
            )

        val vaultClient = provider.createClient()

        val cloud = cloudConfigurationManager.getTenant(cloudName)

        assertTrue(cloud.configurations.any { it.name == "vault-unseal-key-0" })
        assertTrue(cloud.configurations.any { it.name == "vault-unseal-key-1" })
        assertTrue(cloud.configurations.any { it.name == "vault-unseal-key-2" })
        assertTrue(cloud.configurations.any { it.name == "vault-unseal-key-3" })
        assertTrue(cloud.configurations.any { it.name == "vault-unseal-key-4" })
        assertTrue(cloud.configurations.any { it.name == "vault-root-token" })

        assertFalse(vaultClient.opsForSys().unsealStatus.isSealed)
        assertTrue(vaultClient.opsForSys().isInitialized)

        val vaultClient1 = provider.createClient()
        assertFalse(vaultClient1.opsForSys().unsealStatus.isSealed)
    }
}
