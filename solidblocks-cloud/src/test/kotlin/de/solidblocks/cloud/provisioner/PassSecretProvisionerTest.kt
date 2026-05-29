package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.provisioner.pass.PassSecretProvisioner
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.secret.RandomSecret
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.utils.DEFAULT_PASS_DIR
import de.solidblocks.cloud.utils.runCommand
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
class PassSecretProvisionerTest {

    @Test
    fun testFlow() {
        val secretPath = "testCloudName/some/extra/path/secret1"

        val randomSecret = GenericSecret<GenericSecretRuntime>(secretPath, RandomSecret(13))
        val staticSecret = GenericSecret<GenericSecretRuntime>(secretPath, StaticSecret { "static-secret" })
        val oneTimeSecret = GenericSecret<GenericSecretRuntime>(
            secretPath,
            OneTimeGeneratedSecret {
                "onetime-secret"
            },
        )
        val provisioner = PassSecretProvisioner(DEFAULT_PASS_DIR)

        runCommand(listOf("pass", "rm", "--force", "--recursive", "testCloudName"))

        runBlocking {
            /*
            // before create
            provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.missing
            }

            val runtimeAfterCreation = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<PassSecretRuntime>>().data
            val runtimeAfterLookup = provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT)!!
            runtimeAfterLookup.secret shouldHaveLength 13
            runtimeAfterLookup.secret shouldBe runtimeAfterCreation.secret

            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            val runtimeAfterSecondApply = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<PassSecretRuntime>>().data
            runtimeAfterSecondApply.secret shouldBe runtimeAfterCreation.secret

            assertSoftly(provisioner.diff(staticSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
            }

            randomSecret.taint()

            val secretAfterTaint = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            runtimeAfterCreation.secret shouldNotBe secretAfterTaint.shouldBeTypeOf<Success<GarageFsBucketRuntime>>().data

            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }
            assertSoftly(provisioner.diff(oneTimeSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            /**
             * overwrite secret
             */
            provisioner.apply(staticSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            provisioner.lookup(staticSecret.asLookup(), TEST_PROVISIONER_CONTEXT)?.secret shouldBe "static-secret"
             */
        }
    }
}
