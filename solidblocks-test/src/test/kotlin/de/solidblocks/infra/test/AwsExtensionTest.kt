package de.solidblocks.infra.test

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class AwsExtensionTest {

  @Test
  fun testS3Bucket(context: SolidblocksTestContext) {
    val aws = context.aws()
    val bucket = aws.createBucket()

    bucket shouldBe "test-${context.testId.lowercase()}"
  }
}
