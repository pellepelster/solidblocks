package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.aws.iam.AwsIamUser
import de.solidblocks.cloud.provisioner.aws.iam.AwsIamUserProvisioner
import de.solidblocks.cloud.utils.Success
import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(SolidblocksTest::class)
class AwsIamUserProvisionerTest {

    private val bucketArn = "arn:aws:s3:::my-test-bucket"

    private fun readOnlyPolicy(bucketArn: String) = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["s3:GetObject", "s3:ListBucket"],
              "Resource": ["$bucketArn", "$bucketArn/*"]
            }
          ]
        }
    """.trimIndent()

    private fun readWritePolicy(bucketArn: String) = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject", "s3:ListBucket"],
              "Resource": ["$bucketArn", "$bucketArn/*"]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun testFlow(context: SolidblocksTestContext) {
        val provisioner = AwsIamUserProvisioner(
            System.getenv("AWS_ACCESS_KEY_ID"),
            System.getenv("AWS_SECRET_ACCESS_KEY"),
        )

        val name = "test-${UUID.randomUUID().toString().lowercase().take(16)}"

        runBlocking {
            provisioner.lookup(AwsIamUser(name, readOnlyPolicy(bucketArn)).asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null

            assertSoftly(provisioner.diff(AwsIamUser(name, readOnlyPolicy(bucketArn)), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.missing
                it.changes.shouldBeEmpty()
            }

            provisioner.apply(AwsIamUser(name, readOnlyPolicy(bucketArn)), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<*>>()

            assertSoftly(provisioner.lookup(AwsIamUser(name, readOnlyPolicy(bucketArn)).asLookup(), TEST_PROVISIONER_CONTEXT)!!) {
                it.name shouldBe name
                it.arn shouldBe "arn:aws:iam::${it.arn.split(":")[4]}:user/$name"
            }

            assertSoftly(provisioner.diff(AwsIamUser(name, readOnlyPolicy(bucketArn)), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // policy change is detected
            assertSoftly(provisioner.diff(AwsIamUser(name, readWritePolicy(bucketArn)), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
                it.changes shouldHaveSize 1
                it.changes[0].name shouldBe "policy"
            }

            provisioner.apply(AwsIamUser(name, readWritePolicy(bucketArn)), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
                .shouldBeTypeOf<Success<*>>()

            assertSoftly(provisioner.diff(AwsIamUser(name, readWritePolicy(bucketArn)), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            // whitespace differences in policy are ignored
            val policyWithExtraWhitespace = readWritePolicy(bucketArn).replace("\"s3:GetObject\"", "\"s3:GetObject\"   ")
            assertSoftly(provisioner.diff(AwsIamUser(name, policyWithExtraWhitespace), TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
                it.changes.shouldBeEmpty()
            }

            provisioner.destroy(AwsIamUser(name, readWritePolicy(bucketArn)), TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT) shouldBe true
            provisioner.lookup(AwsIamUser(name, readWritePolicy(bucketArn)).asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
        }
    }
}
