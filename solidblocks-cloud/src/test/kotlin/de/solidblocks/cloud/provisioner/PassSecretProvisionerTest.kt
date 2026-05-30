package de.solidblocks.cloud.provisioner
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.diffData
import de.solidblocks.cloud.provisioner.garagefs.bucket.GarageFsBucketRuntime
import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.secret.RandomSecret
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.defaultPassDir
import de.solidblocks.cloud.utils.runCommand
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
class PassSecretProvisionerTest {

    @Test
    fun testFlow() {
        val secretPath = "testCloudName/some/extra/path/secret1"

        val randomSecret = GenericSecret<GenericSecretRuntime>(secretPath, RandomSecret(13), true)
        val staticSecret = GenericSecret<GenericSecretRuntime>(secretPath, StaticSecret { "static-secret" }, true)
        val oneTimeSecret = GenericSecret<GenericSecretRuntime>(
            secretPath,
            OneTimeGeneratedSecret {
                "onetime-secret"
            },
            true,
        )
        val provisioner = PassSecretProvisioner(defaultPassDir())

        runCommand(listOf("pass", "rm", "--force", "--recursive", "testCloudName"))

        runBlocking {
            // before create
            provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.missing
            }

            val runtimeAfterCreation = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<GenericSecretRuntime>>().data
            val runtimeAfterLookup = provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT)!!
            runtimeAfterLookup.secret shouldHaveLength 13
            runtimeAfterLookup.secret shouldBe runtimeAfterCreation.secret

            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            val runtimeAfterSecondApply = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<GenericSecretRuntime>>().data
            runtimeAfterSecondApply.secret shouldBe runtimeAfterCreation.secret

            assertSoftly(provisioner.diff(staticSecret, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.has_changes
            }

            TEST_PROVISIONER_CONTEXT.taintedResources.add(randomSecret)

            val secretAfterTaint = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            runtimeAfterCreation.secret shouldNotBe secretAfterTaint.shouldBeTypeOf<Success<GarageFsBucketRuntime>>().data

            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }
            assertSoftly(provisioner.diff(oneTimeSecret, TEST_PROVISIONER_CONTEXT).diffData()) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            /**
             * overwrite secret
             */
            provisioner.apply(staticSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            provisioner.lookup(staticSecret.asLookup(), TEST_PROVISIONER_CONTEXT)?.secret shouldBe "static-secret"
        }
    }
}
