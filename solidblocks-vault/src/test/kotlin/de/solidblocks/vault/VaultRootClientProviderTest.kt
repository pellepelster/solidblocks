package de.solidblocks.vault

import de.solidblocks.cloud.config.CloudConfigurationManager
import de.solidblocks.cloud.config.SolidblocksDatabase
import de.solidblocks.test.KDockerComposeContainer
import de.solidblocks.test.TestConstants.TEST_DB_JDBC_URL
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import java.io.File
import java.util.*

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

        private fun vaultAddress() = "http://localhost:${environment.getServicePort("vault", 8200)}"
    }

    @Test
    fun testCreateClient() {
        val db = SolidblocksDatabase(TEST_DB_JDBC_URL)
        db.ensureDBSchema()

        val configurationManager = CloudConfigurationManager(db.dsl)

        val cloudName = UUID.randomUUID().toString()
        val environmentName = UUID.randomUUID().toString()

        configurationManager.createCloud(cloudName, "domain1", emptyList())
        configurationManager.createEnvironment(cloudName, environmentName)

        val vaultClientProvider = VaultRootClientProvider(cloudName, environmentName, configurationManager, vaultAddress())
        assertThat(vaultClientProvider.createClient().list("/")).hasSize(0)
    }
}
