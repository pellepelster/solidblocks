package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.test.SolidblocksTestDatabaseExtension
import de.solidblocks.vault.VaultRootClientProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.util.*

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@Testcontainers
@ExtendWith(SolidblocksTestDatabaseExtension::class)
class VaultRootClientProviderTest {

    companion object {
        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withPull(true)
                    withExposedService("vault", 8200)
                    start()
                }

        fun vaultAddress() = "http://localhost:${environment.getServicePort("vault", 8200)}"
    }

    @Test
    fun testInitAndUnseal(solidblocksDatabase: SolidblocksDatabase) {

        val cloudRepository = CloudRepository(solidblocksDatabase.dsl)
        val environmentRepository = EnvironmentRepository(solidblocksDatabase.dsl, cloudRepository)

        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()

        cloudRepository.createCloud(cloudName, "domain1", emptyList())
        environmentRepository.createEnvironment(cloudName, environmentName)

        val provider = VaultRootClientProvider(cloudName, environmentName, environmentRepository, vaultAddress())

        val environmentBefore = environmentRepository.getEnvironment(cloudName, environmentName)
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-0" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-1" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-2" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-3" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-4" })

        val vaultClient = provider.createClient()
        val environment = environmentRepository.getEnvironment(cloudName, environmentName)

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
