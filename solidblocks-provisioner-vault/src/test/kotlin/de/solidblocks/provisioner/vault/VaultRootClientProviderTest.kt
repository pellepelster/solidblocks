package de.solidblocks.provisioner.vault

import de.solidblocks.cloud.config.CloudConfigurationContext
import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.provisioner.vault.provider.VaultRootClientProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

class KDockerComposeContainer(file: File) : DockerComposeContainer<KDockerComposeContainer>(file)

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestApplicationContext::class, LiquibaseAutoConfiguration::class])
@AutoConfigureTestDatabase
@Testcontainers
class VaultRootClientProviderTest(
    @Autowired
    val cloudConfigurationManager: CloudConfigurationManager,
    @Autowired
    val configurationContext: CloudConfigurationContext,
    @Autowired
    val provider: VaultRootClientProvider
) {

    companion object {
        @Container
        val environment: DockerComposeContainer<*> =
            KDockerComposeContainer(File("src/test/resources/docker-compose.yml"))
                .apply {
                    withExposedService("vault", 8200)
                    start()
                }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("vault.addr") { "http://localhost:${environment.getServicePort("vault", 8200)}" }
        }
    }

    @Test
    fun testInitAndUnseal() {
        val environmentBefore = cloudConfigurationManager.environmentByName(
            configurationContext.cloudName,
            configurationContext.environmentName
        )
        assertTrue(environmentBefore!!.configValues.none { it.name == "vault-unseal-key-0" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-1" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-2" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-3" })
        assertTrue(environmentBefore.configValues.none { it.name == "vault-unseal-key-4" })


        val vaultClient = provider.createClient()
        val environment = cloudConfigurationManager.environmentByName(
            configurationContext.cloudName,
            configurationContext.environmentName
        )

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
