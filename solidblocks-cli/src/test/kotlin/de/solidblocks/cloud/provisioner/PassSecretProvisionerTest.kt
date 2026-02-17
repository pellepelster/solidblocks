package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.provisioner.pass.Secret
import de.solidblocks.cloud.utils.runCommand
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PassSecretProvisionerTest {

  @Test
  fun testFlow() {
    val resource = Secret("some/extra/path/secret1", 13)
    val provisioner = PassSecretProvisioner()

    runCommand(listOf("pass", "rm", "--force", "--recursive", "testCloudName"))

    runBlocking {
      // before create
      provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
      assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
        it!!.status shouldBe ResourceDiffStatus.missing
      }

      provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
      val generatedSecret = provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!
      generatedSecret.secret shouldHaveLength 13

      assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
        it!!.status shouldBe ResourceDiffStatus.up_to_date
      }

      provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
      val secretAfterSecondApply =
          provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!
      generatedSecret.secret shouldBe secretAfterSecondApply.secret

      resource.taint()

      assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
        it!!.status shouldBe ResourceDiffStatus.up_to_date
      }

      val secretAfterTaint =
          provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)!!
      generatedSecret.secret shouldNotBe secretAfterTaint.result!!.secret

      assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
        it!!.status shouldBe ResourceDiffStatus.up_to_date
      }
    }
  }
}
