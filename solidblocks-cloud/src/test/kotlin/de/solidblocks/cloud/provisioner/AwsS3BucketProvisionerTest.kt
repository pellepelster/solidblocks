package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.aws.s3.AwsS3Bucket
import de.solidblocks.cloud.provisioner.aws.s3.AwsS3BucketProvisioner
import de.solidblocks.cloud.utils.Success
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class AwsS3BucketProvisionerTest {

    @Test
    fun testFlow(context: SolidblocksTestContext) {
        val region = System.getenv("AWS_REGION")
        val provisioner = AwsS3BucketProvisioner(
            System.getenv("AWS_ACCESS_KEY_ID"),
            System.getenv("AWS_SECRET_ACCESS_KEY"),
            region,
        )

        val name = "test-${UUID.randomUUID().toString().lowercase().take(16)}"

        runBlocking {
            provisioner.lookup(AwsS3Bucket(name, region).asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null

            assertSoftly(provisioner.diff(AwsS3Bucket(name, region), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.missing
                it.changes.shouldBeEmpty()
            }

            provisioner.apply(AwsS3Bucket(name, region), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<*>>()

            assertSoftly(provisioner.lookup(AwsS3Bucket(name, region).asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
                it.name shouldBe name
                it.region shouldBe region
            }

            assertSoftly(provisioner.diff(AwsS3Bucket(name, region), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // TODO fix flakey bucket deletion
            // provisioner.destroy(AwsS3Bucket(name, region), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT) shouldBe true
            // provisioner.lookup(AwsS3Bucket(name, region).asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
        }
    }
}
