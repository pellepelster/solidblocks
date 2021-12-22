package de.solidblocks.service.vault

import de.solidblocks.test.SolidblocksLocalEnv
import de.solidblocks.test.SolidblocksLocalEnvExtension
import de.solidblocks.vault.VaultManager
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultKeyValueOperationsSupport
import org.springframework.vault.core.VaultTemplate
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.*

@ExtendWith(SolidblocksLocalEnvExtension::class)
class VaultServiceManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testKeepsDataAfterRestart(solidblocksLocalEnv: SolidblocksLocalEnv) {

        val reference = solidblocksLocalEnv.createServiceReference("vault")
        // assertThat(solidblocksLocalEnv.bootstrapService(reference)).isTrue

        val testDir = "/tmp/${UUID.randomUUID()}"

        logger.info { "creating test dir '$testDir'" }
        File(testDir).mkdirs()
        Files.setPosixFilePermissions(File(testDir).toPath(), PosixFilePermissions.fromString("rwxrwxrwx"))

        val service = VaultServiceManager(
            solidblocksLocalEnv.reference.asService("service1"),
            testDir,
            VaultManager(solidblocksLocalEnv.vaultAddress, solidblocksLocalEnv.rootToken, solidblocksLocalEnv.reference)
        )

        assertThat(service.start()).isTrue

        val vaultTemplate = VaultTemplate(
            VaultEndpoint.from(URI.create(service.vaultAddress)),
            TokenAuthentication(service.loadCredentials()!!.rootToken)
        )
        vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
            .put("test", mapOf("foo" to "bar"))
        service.stop()

        assertThat(service.isRunning()).isFalse
        assertThat(service.start()).isTrue

        val testData = vaultTemplate.opsForKeyValue("cubbyhole", VaultKeyValueOperationsSupport.KeyValueBackend.KV_2)
            .get("test", Map::class.java)
        assertThat(testData!!.data).isEqualTo(mapOf("foo" to "bar"))

        service.backup()

        service.stop()
    }
}
