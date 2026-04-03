package de.solidblocks.cloud

import de.solidblocks.cloud.configuration.model.CloudConfiguration
import de.solidblocks.cloud.provisioner.ProvisionerContext
import de.solidblocks.cloud.services.s3.S3ServiceManager
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketAccessKeyConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.utils.Error
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.mockk
import org.junit.jupiter.api.Test

class S3ServiceConfigurationManagerTest {

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
                    12,
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
                    12,
                    listOf(
                        S3ServiceBucketConfiguration(
                            "bucket1",
                            false,
                            listOf(
                                S3ServiceBucketAccessKeyConfiguration("name1"),
                                S3ServiceBucketAccessKeyConfiguration("name1"),
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
