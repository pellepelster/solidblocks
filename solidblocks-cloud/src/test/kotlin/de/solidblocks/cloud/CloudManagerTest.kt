package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.setPosixFilePermissions
import kotlin.io.path.writeText

class CloudManagerTest {

    @Test
    fun testEmptyConfiguration() {
        val manager = "".createManager()
        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe "cloud configuration was empty"
    }

    @Test
    fun testNoName() {
        val manager =
            """
        providers:
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe "key 'name' not found at line 1 colum 1"
    }

    @Test
    fun testNoProviders() {
        val manager =
            """
        name: cloud1
        providers:
            - type: something
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe
                "unknown type 'something', possible types are 'hcloud', 'pass', 'ssh_key' at line 3 colum 7"
    }

    @Test
    fun testNoSSHKeyProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe "more than one or no provider for ssh keys found (0), please register exactly one. available types are: 'ssh_key'"
    }

    @Test
    fun testS3BackupProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
            - type: backup_s3
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Success<CloudConfigurationRuntime>>()

        result.data.providers shouldHaveSize 4
    }

    @Test
    fun testSSHKeyTooOpen() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
              private_key: "test_ssh_open_permissions.key"
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldMatch "permissions for ssh key '.*' are too open, should be owner r/w only"
    }

    @Test
    fun testNoSecretProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: ssh_key
              private_key: "test_ssh.key"
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe
                "more than one or no secret provider found (0), please register exactly one. available types are: 'pass'"
    }

    @Test
    fun testNoCloudProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: pass
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe
                "more than one or no cloud provider found (0), please register exactly one. available types are: 'hcloud'"
    }
}

fun String.createManager(): CloudManager {
    val tempDir = Files.createTempDirectory("test")

    val rawSshKey =
        CloudManagerTest::class
            .java
            .classLoader
            .getResourceAsStream("test_ed25519.key")
            .bufferedReader(Charsets.UTF_8)
            .use(BufferedReader::readText)

    tempDir.resolve("test_ssh_open_permissions.key").writeText(rawSshKey)
    tempDir.resolve("test_ssh.key").also {
        it.writeText(rawSshKey)
        it.toAbsolutePath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))
    }

    val tempFile = tempDir.resolve("cloud.yaml").also { it.writeText(this) }.toFile()

    return CloudManager(tempFile)
}
