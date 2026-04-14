package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketRuntime
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.utils.DEFAULT_PASS_DIR
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.runCommand
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
class PassSecretProvisionerTest {

    @Test
    fun testFlow() {
        val secretPath = "testCloudName/some/extra/path/secret1"
        val resource = PassSecret(secretPath, 13)
        val newSecret = PassSecret(secretPath, secret = {
            "new-secret"
        })
        val provisioner = PassSecretProvisioner(DEFAULT_PASS_DIR)

        runCommand(listOf("pass", "rm", "--force", "--recursive", "testCloudName"))

        runBlocking {
            // before create
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.missing
            }

            provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            val generatedSecret = provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!
            generatedSecret.secret shouldHaveLength 13

            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            assertSoftly(provisioner.diff(newSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
            }

            provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            val secretAfterSecondApply =
                provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)!!
            generatedSecret.secret shouldBe secretAfterSecondApply.secret

            resource.taint()

            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            val secretAfterTaint = provisioner.apply(resource, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            generatedSecret.secret shouldNotBe
                secretAfterTaint.shouldBeTypeOf<Success<GarageFsBucketRuntime>>().data

            assertSoftly(provisioner.diff(resource, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            /**
             * overwrite secret
             */
            provisioner.apply(newSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            provisioner.lookup(resource.asLookup(), TEST_PROVISIONER_CONTEXT)?.secret shouldBe "new-secret"
        }
    }
}
