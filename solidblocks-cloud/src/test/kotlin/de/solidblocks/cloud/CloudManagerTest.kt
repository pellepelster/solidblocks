package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.providers.backup.aws.S3BackupProviderConfigurationRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
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
        error.error shouldBe "key 'name' not found at line 1 column 1"
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
            "unknown type 'something', possible types are 'hcloud', 'protonpass', 'pass', 'ssh_key', 'backup_aws_s3', 'backup_local', 'github' at line 3 column 7"
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
        error.error shouldBe "invalid number of 'ssh_key' providers, found 0 but expected exactly 1. available types are: 'ssh_key'"
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
            - type: backup_aws_s3
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Success<CloudConfigurationRuntime>>()
        result.data.providers shouldHaveSize 4
        result.data.backupProviderRuntime().shouldBeTypeOf<S3BackupProviderConfigurationRuntime>()
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
    fun testDefaultToCloudSSHKey() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
            - type: backup_local
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Success<CloudConfigurationRuntime>>()
        result.data.providers shouldHaveSize 4

        val managerNoDefaultSSHKey =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
            - type: backup_local
        """
                .trimIndent()
                .createManager("some-other.key")

        val error = managerNoDefaultSSHKey.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldStartWith "encrypted private keys are currently not supported"
    }

    @Test
    fun testDuplicateDefaultProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldStartWith "found more then one default for provider of type 'ssh_key'."
    }

    @Test
    fun testDuplicateProviderName() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
              name: foo-bar
            - type: ssh_key
              name: foo-bar
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "found duplicate provider configuration for type 'ssh_key' with name 'foo-bar'."
    }

    @Test
    fun testDuplicateServiceName() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key

        services:
            - type: postgresql
              name: database1
            - type: postgresql
              name: database1
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "found duplicate service configuration for name 'database1'."
    }

    @Test
    fun testInvalidRootDomain() {
        val manager =
            """
        name: cloud1
        root_domain: yolo.de
        providers:
            - type: hcloud
            - type: pass
            - type: backup_local
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "no zone found for root domain 'yolo.de', please make sure that the zone can be managed by the configured cloud provider"
    }

    @Test
    fun testMoreThanOneCloudProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: hcloud
              name: "foo-bar"
            - type: pass
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "invalid number of 'cloud' providers, found 2 but expected at most 1. available types are: 'hcloud'"
    }

    @Test
    fun testMoreThanOneSSHKeyProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
              name: "foo-bar"
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "invalid number of 'ssh_key' providers, found 2 but expected exactly 1. available types are: 'ssh_key'"
    }

    @Test
    fun testServiceIsMissingProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
            - type: backup_local
        services:
            - type: github_runner
              name: runner1
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "service 'runner1' needs a 'github' provider, available types are: 'github'"
    }

    @Test
    fun testServiceIsMissingProviderCategory() {
        val manager =
            """
        name: cloud1
        providers:
            - type: github
              github_url: "https://github.com/pellepelster/solidblocks"
            - type: pass
            - type: ssh_key
            - type: backup_local
        services:
            - type: github_runner
              name: runner1
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "service 'runner1' needs a 'cloud' provider, available types are: 'hcloud'"
    }

    @Test
    fun testMoreThanOneSecretProvider() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: backup_local
            - type: pass
              name: "foo-bar"
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()

        result.error shouldBe "invalid number of 'secret' providers, found 2 but expected at most 1. available types are: 'protonpass', 'pass'"
    }

    @Test
    fun testSSHKeyTooOpen() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: backup_local
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
            - type: backup_local
            - type: ssh_key
        services:
            - type: postgresql
              name: service1
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe
            "service 'service1' needs a 'secret' provider, available types are: 'protonpass', 'pass'"
    }

    @Test
    fun testNoServices() {
        val manager =
            """
        name: cloud1
        providers:
            - type: ssh_key
        """
                .trimIndent()
                .createManager()

        val cloud = manager.validate().shouldBeTypeOf<Success<CloudConfigurationRuntime>>().data
        cloud.providers shouldHaveSize 1
        cloud.services shouldHaveSize 0
    }

    @Test
    fun `github runner needs github provider`() {
        val manager =
            """
        name: cloud1
        providers:
            - type: ssh_key
        services:
            - type: github_runner
              name: "runner1"
        """
                .trimIndent()
                .createManager()

        val result = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        result.error shouldBe "service 'runner1' needs a 'cloud' provider, available types are: 'hcloud'"
    }

    @Test
    fun `service needs backup provider`() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: pass
            - type: ssh_key
        services:
            - type: postgresql
              name: service1
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe
            "service 'service1' needs a 'backup' provider, available types are: 'backup_aws_s3', 'backup_local'"
    }

    @Test
    fun `service needs secret provider`() {
        val manager =
            """
        name: cloud1
        providers:
            - type: hcloud
            - type: backup_local
            - type: ssh_key
        services:
            - type: postgresql
              name: service1
        """
                .trimIndent()
                .createManager()

        val error = manager.validate().shouldBeTypeOf<Error<CloudConfigurationRuntime>>()
        error.error shouldBe
            "service 'service1' needs a 'secret' provider, available types are: 'protonpass', 'pass'"
    }
}

fun String.createManager(sshKeyName: String = "cloud1.key"): CloudManager {
    val tempDir = Files.createTempDirectory("test")

    val rawSshKey =
        CloudManagerTest::class
            .java
            .classLoader
            .getResourceAsStream("test_ed25519.key")
            .bufferedReader(Charsets.UTF_8)
            .use(BufferedReader::readText)

    tempDir.resolve("test_ssh_open_permissions.key").writeText(rawSshKey)
    tempDir.resolve(sshKeyName).also {
        it.writeText(rawSshKey)
        it.toAbsolutePath().setPosixFilePermissions(PosixFilePermissions.fromString("rw-------"))
    }

    val tempFile = tempDir.resolve("cloud.yaml").also { it.writeText(this) }.toFile()

    return CloudManager(tempFile)
}
