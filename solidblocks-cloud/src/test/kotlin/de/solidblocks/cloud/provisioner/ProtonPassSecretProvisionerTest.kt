package de.solidblocks.cloud.provisioner

import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.TEST_PROVISIONER_CONTEXT
import de.solidblocks.cloud.api.ResourceDiffStatus
import de.solidblocks.cloud.provisioner.protonpass.ProtonPassSecretProvisioner
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.secret.RandomSecret
import de.solidblocks.cloud.provisioner.secret.StaticSecret
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.protonPassItemDelete
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
class ProtonPassSecretProvisionerTest {

    private val vaultName = System.getenv("BLCKS_TEST_PROTONPASS_VAULT") ?: "Personal"

    @Test
    fun testFlow() {
        val secretTitle = "blcks-test/some/extra/path/secret1"

        val randomSecret = GenericSecret<GenericSecretRuntime>(secretTitle, RandomSecret(13), true)
        val staticSecret = GenericSecret<GenericSecretRuntime>(secretTitle, StaticSecret { "static-secret" }, true)
        val oneTimeSecret = GenericSecret<GenericSecretRuntime>(
            secretTitle,
            OneTimeGeneratedSecret {
                "onetime-secret"
            },
            true,
        )
        val provisioner = ProtonPassSecretProvisioner(vaultName)

        runBlocking {
            // ensure a clean state
            provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT)?.let {
                protonPassItemDelete(it.shareId, it.itemId)
            }

            // before create
            provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT) shouldBe null
            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.missing
            }

            val runtimeAfterCreation = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<GenericSecretRuntime>>().data
            val runtimeAfterLookup = provisioner.lookup(randomSecret.asLookup(), TEST_PROVISIONER_CONTEXT)!!
            runtimeAfterLookup.secret shouldHaveLength 13
            runtimeAfterLookup.secret shouldBe runtimeAfterCreation.secret

            // an ephemeral secret that already exists stays unchanged
            assertSoftly(provisioner.diff(randomSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            val runtimeAfterSecondApply = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<GenericSecretRuntime>>().data
            runtimeAfterSecondApply.secret shouldBe runtimeAfterCreation.secret

            // a static secret with a different value reports pending changes
            assertSoftly(provisioner.diff(staticSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.has_changes
            }

            // tainting forces a new ephemeral secret
            randomSecret.taint()
            val secretAfterTaint = provisioner.apply(randomSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT).shouldBeInstanceOf<Success<GenericSecretRuntime>>().data
            runtimeAfterCreation.secret shouldNotBe secretAfterTaint.secret

            assertSoftly(provisioner.diff(oneTimeSecret, TEST_PROVISIONER_CONTEXT)) {
                it.status shouldBe ResourceDiffStatus.up_to_date
            }

            // overwrite secret with a static value
            provisioner.apply(staticSecret, TEST_PROVISIONER_CONTEXT, TEST_LOG_CONTEXT)
            provisioner.lookup(staticSecret.asLookup(), TEST_PROVISIONER_CONTEXT)?.secret shouldBe "static-secret"

            // clean up
            provisioner.lookup(staticSecret.asLookup(), TEST_PROVISIONER_CONTEXT)?.let {
                protonPassItemDelete(it.shareId, it.itemId)
            }
        }
    }
}
