package de.solidblocks.vault

import de.solidblocks.test.KDockerComposeContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File

@Testcontainers
class InitializingVaultManagerTest {

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
    fun testInitAndUnseal() {
        val initializingVaultManager = InitializingVaultManager(vaultAddress())

        assertThat(initializingVaultManager.isInitialized()).isFalse
        val result = initializingVaultManager.initialize()

        assertThat(initializingVaultManager.isInitialized()).isTrue
        assertThat(initializingVaultManager.isSealed()).isTrue

        assertThat(initializingVaultManager.unseal(result)).isTrue
        assertThat(initializingVaultManager.isSealed()).isFalse

        // initializingVaultManager.seal()
        // assertThat(initializingVaultManager.isSealed()).isTrue
    }
}
