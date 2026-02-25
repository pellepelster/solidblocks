package de.solidblocks.cloud.services

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.configuration.model.CloudConfigurationRuntime
import de.solidblocks.cloud.services.s3.S3ServiceConfigurationManager
import de.solidblocks.cloud.services.s3.model.S3ServiceBucketConfiguration
import de.solidblocks.cloud.services.s3.model.S3ServiceConfiguration
import de.solidblocks.cloud.utils.Error
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test

class S3ServiceConfigurationManagerTest {

  @Test
  fun testDuplicateBucketName() {
    val result =
        S3ServiceConfigurationManager(
                CloudConfigurationRuntime(
                    "name1",
                    "blcks-test.de",
                    emptyList(),
                    emptyList(),
                ),
            )
            .validatConfiguration(
                S3ServiceConfiguration(
                    "bucket1",
                    listOf(
                        S3ServiceBucketConfiguration("bucket1", false),
                        S3ServiceBucketConfiguration("bucket1", false),
                    ),
                ),
                TEST_LOG_CONTEXT,
            )
            .shouldBeTypeOf<Error<Unit>>()

    result.error shouldBe "duplicated configuration for bucket 'bucket1'"
  }
}
