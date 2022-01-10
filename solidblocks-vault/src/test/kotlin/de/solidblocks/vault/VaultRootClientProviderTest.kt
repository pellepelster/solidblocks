package de.solidblocks.vault

import de.solidblocks.base.EnvironmentReference
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.SolidblocksDatabase
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
                    withExposedService("vault", 8200)
                    start()
                }

        private fun vaultAddress() = "http://localhost:${environment.getServicePort("vault", 8200)}"
    }

    @Test
    fun testCreateClient() {
        val db = SolidblocksDatabase(TEST_DB_JDBC_URL())
        db.ensureDBSchema()

        val cloudRepository = CloudRepository(db.dsl)
        val environmentRepository = EnvironmentRepository(db.dsl, cloudRepository)

        val reference = EnvironmentReference(UUID.randomUUID().toString(), UUID.randomUUID().toString())

        cloudRepository.createCloud(reference, "domain1", emptyList())
        environmentRepository.createEnvironment(reference)

        val vaultClientProvider =
            VaultRootClientProvider(reference, environmentRepository, vaultAddress())
        assertThat(vaultClientProvider.createClient().list("/")).hasSize(0)
    }
}
