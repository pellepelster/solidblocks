package de.solidblocks.vault.agent

import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import de.solidblocks.test.TestUtils.initWorldReadableTempDir
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultKeyValueOperationsSupport
import org.springframework.vault.core.VaultTemplate
import java.net.URI
import java.util.*

@ExtendWith(DevelopmentEnvironmentExtension::class)
class VaultAgentTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testKeepsDataAfterRestart(developmentEnvironment: DevelopmentEnvironment) {

        val service = "vault-${UUID.randomUUID()}"
        if (!developmentEnvironment.createVaultService(service)) {
            throw RuntimeException("error creating service")
        }

        val tempDir = initWorldReadableTempDir(service)
        val reference = developmentEnvironment.environmentRef.toService(service)

        val serviceManager = VaultAgent(
            reference,
            tempDir,
            developmentEnvironment.minioAddress,
            developmentEnvironment.vaultAddress,
            developmentEnvironment.rootToken
        )

        assertThat(serviceManager.start()).isTrue

        val vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(serviceManager.vaultAddress)),
            TokenAuthentication(serviceManager.loadCredentials()!!.rootToken)
        )
        vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
            .put("test", mapOf("foo" to "bar"))

        serviceManager.stop()

        assertThat(serviceManager.isRunning()).isFalse
        assertThat(serviceManager.start()).isTrue

        val testDataAfterRestart =
            vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
                .get("test", Map::class.java)
        assertThat(testDataAfterRestart!!.data).isEqualTo(mapOf("foo" to "bar"))

        serviceManager.stop()
    }

    @Test
    fun testRestoreDataIntoService(developmentEnvironment: DevelopmentEnvironment) {

        val service = "vault-${UUID.randomUUID()}"
        if (!developmentEnvironment.createVaultService(service)) {
            throw RuntimeException("error creating service")
        }

        val tempDir = initWorldReadableTempDir(service)
        val reference = developmentEnvironment.environmentRef.toService(service)

        val serviceManager = VaultAgent(
            reference,
            tempDir,
            developmentEnvironment.minioAddress,
            developmentEnvironment.vaultAddress,
            developmentEnvironment.rootToken
        )

        assertThat(serviceManager.start()).isTrue

        val vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(serviceManager.vaultAddress)),
            TokenAuthentication(serviceManager.loadCredentials()!!.rootToken)
        )
        vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
            .put("test", mapOf("foo" to "bar"))

        assertThat(serviceManager.backup()).isTrue

        vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
            .put("test", mapOf("foo" to "newBar"))

        val testDataBeforeRestore =
            vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
                .get("test", Map::class.java)
        assertThat(testDataBeforeRestore!!.data).isEqualTo(mapOf("foo" to "newBar"))

        assertThat(serviceManager.restore()).isTrue

        val testDataAfterRestore =
            vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
                .get("test", Map::class.java)
        assertThat(testDataAfterRestore!!.data).isEqualTo(mapOf("foo" to "bar"))

        serviceManager.stop()
    }
}
