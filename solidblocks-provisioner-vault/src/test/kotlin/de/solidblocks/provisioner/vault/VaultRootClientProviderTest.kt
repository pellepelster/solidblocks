package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.SolidblocksDatabase
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@Testcontainers
class VaultRootClientProviderTest {

    companion object {
        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withExposedService("vault", 8200)
                    start()
                }

        fun vaultAddress() = "http://localhost:${environment.getServicePort("vault", 8200)}"
    }

    @Test
    fun testInitAndUnseal() {

        val db = SolidblocksDatabase("jdbc:derby:memory:myDB;create=true")
        db.ensureDBSchema()

        val configurationManager = CloudConfigurationManager(db.dsl)

        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()

        configurationManager.createCloud(cloudName, "domain1", emptyList())
        configurationManager.createEnvironment(cloudName, environmentName)

        val cloudConfigurationContext = CloudConfigurationContext(
            configurationManager.environmentByName(cloudName, environmentName)!!
        )

        val provider = VaultRootClientProvider(cloudName, environmentName, configurationManager, vaultAddress())

        val environmentBefore = configurationManager.environmentByName(cloudName, environmentName)
        assertTrue(environmentBefore!!.configValues.none { it.name == "vault-unseal-key-0" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-1" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-2" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-3" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-4" })

        val vaultClient = provider.createClient()
        val environment = configurationManager.environmentByName(cloudName, environmentName)

        assertTrue(environment!!.configValues.any { it.name == "vault-unseal-key-0" })
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
