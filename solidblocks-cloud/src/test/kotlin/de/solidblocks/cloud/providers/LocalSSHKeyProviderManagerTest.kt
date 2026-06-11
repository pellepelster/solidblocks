package de.solidblocks.cloud.providers

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TestContextUtils
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderConfiguration
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderConfigurationRuntime
import de.solidblocks.cloud.providers.sshkey.LocalSSHKeyProviderManager
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.ssh.KeyType
import de.solidblocks.ssh.keyType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.Path
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText

class LocalSSHKeyProviderManagerTest {

    val provider = LocalSSHKeyProviderManager()

    val tempDir = Files.createTempDirectory("test")

    @Test
    fun `loads default key for cloud`() {
        val rawSshKey = TestContextUtils::class.java.getResource("/test_ed25519.key").readText()
        tempDir.resolve("cloud1.key").also {
            it.writeText(rawSshKey)
            it.toAbsolutePath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))
        }

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", null),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Success<LocalSSHKeyProviderConfigurationRuntime>>()
        result.data.keyPair.keyType() shouldBe KeyType.ed25519
    }

    @Test
    fun `key permissions too open`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_ed25519.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-rw-rw-"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Error<LocalSSHKeyProviderConfigurationRuntime>>()
        result.error shouldContain "are too open, should be owner r/w only"
    }

    @Test
    fun `rsa encrypted`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_rsa_encrypted.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Error<LocalSSHKeyProviderConfigurationRuntime>>()
        result.error shouldContain "encrypted private keys are currently not supported"
    }

    @Test
    fun `ed25519 encrypted`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_ed25519_encrypted.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Error<LocalSSHKeyProviderConfigurationRuntime>>()
        result.error shouldContain "encrypted private keys are currently not supported"
    }

    @Test
    fun `ecdsa encrypted`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_ecdsa_encrypted.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Error<LocalSSHKeyProviderConfigurationRuntime>>()
        result.error shouldContain "encrypted private keys are currently not supported"
    }

    @Test
    fun `ecdsa`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_ecdsa.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Error<LocalSSHKeyProviderConfigurationRuntime>>()
        result.error shouldContain "unsupported key type"
    }

    @Test
    fun `rsa`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_rsa.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Success<LocalSSHKeyProviderConfigurationRuntime>>()
        result.data.keyPair.keyType() shouldBe KeyType.rsa
    }

    @Test
    fun `ed25519`() {
        val sshKeyPath = TestContextUtils::class.java.getResource("/test_ed25519.key").toURI().path
        Path(sshKeyPath).setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))

        val result = provider.validateConfiguration(
            LocalSSHKeyProviderConfiguration("localsshprovider1", sshKeyPath.toString()),
            CloudConfigurationContext(EnvironmentContext("cloud1", "default"), tempDir),
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Success<LocalSSHKeyProviderConfigurationRuntime>>()
        result.data.keyPair.keyType() shouldBe KeyType.ed25519
    }
}
