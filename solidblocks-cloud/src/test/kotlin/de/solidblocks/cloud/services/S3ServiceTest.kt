package de.solidblocks.cloud.services

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.provisioner.context.ProvisionerContext
import de.solidblocks.cloud.services.s3.S3ServiceManager
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketAccessKeyConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfigurationFactory
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.yamlParse
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import org.junit.jupiter.api.Test

class S3ServiceTest {

    @Test
    fun testParse() {
        val ymlRaw =
            """
        name: "name1"
        buckets:
            - name: bucket1
        """
                .trimIndent()

        val yaml = yamlParse(ymlRaw).shouldBeTypeOf<Success<YamlNode>>()
        val result = S3ServiceConfigurationFactory().parse(yaml.data)
        val configuration = result.shouldBeTypeOf<Success<S3ServiceConfiguration>>()
        configuration.data.name shouldBe "name1"
        configuration.data.buckets shouldHaveSize 1
    }

    @Test
    fun testDuplicateBucketName() {
        val cloud =
            CloudConfiguration(
                "name1",
                "blcks-test.de",
                emptyList(),
                emptyList(),
            )

        val result =
            S3ServiceManager()
                .validateConfiguration(
                    0,
                    cloud,
                    S3ServiceConfiguration(
                        "bucket1",
                        InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                        BackupConfig(16, 7),
                        listOf(
                            S3ServiceBucketConfiguration("bucket1", false, emptyList(), emptyList()),
                            S3ServiceBucketConfiguration("bucket1", false, emptyList(), emptyList()),
                        ),
                    ),
                    mockk<ProvisionerContext>(),
                    TEST_LOG_CONTEXT,
                )
                .shouldBeTypeOf<Error<Unit>>()

        result.error shouldBe
            "duplicated configuration for bucket with name 'bucket1', ensure that the bucket names are unique"
    }

    @Test
    fun testDuplicateBucketAccessKeyName() {
        val cloud =
            CloudConfiguration(
                "name1",
                "blcks-test.de",
                emptyList(),
                emptyList(),
            )

        val result =
            S3ServiceManager()
                .validateConfiguration(
                    0,
                    cloud,
                    S3ServiceConfiguration(
                        "bucket1",
                        InstanceConfig(16, HetznerLocation.fsn1, HetznerServerType.cx23),
                        BackupConfig(16, 7),
                        listOf(
                            S3ServiceBucketConfiguration(
                                "bucket1",
                                false,
                                listOf(
                                    S3ServiceBucketAccessKeyConfiguration("name1", true, true, true),
                                    S3ServiceBucketAccessKeyConfiguration("name1", true, true, true),
                                ),
                                emptyList(),
                            ),
                        ),
                    ),
                    mockk<ProvisionerContext>(),
                    TEST_LOG_CONTEXT,
                )
                .shouldBeTypeOf<Error<Unit>>()

        result.error shouldBe
            "duplicated access key with name 'name1' found for bucket 'bucket1', ensure that the access key names are unique"
    }
}
