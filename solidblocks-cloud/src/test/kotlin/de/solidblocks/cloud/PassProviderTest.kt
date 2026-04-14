package de.solidblocks.cloud

import de.solidblocks.cloud.providers.pass.PassProviderConfiguration
import de.solidblocks.cloud.providers.pass.PassProviderManager
import de.solidblocks.cloud.providers.pass.PassProviderRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
class PassProviderTest {

    val provider = PassProviderManager()

    @Test
    fun testPassProviderDefaultDir() {
        val result = provider.validateConfiguration(PassProviderConfiguration("passprovider1", null), TEST_CLOUD_CONFIGURATION_CONTEXT, TEST_LOG_CONTEXT).shouldBeTypeOf<Success<PassProviderRuntime>>()
        result.data.passwordStoreDir shouldBe "/home/pelle/.password-store"
    }

    @Test
    fun testPassProviderInvalidDir() {
        val result = provider.validateConfiguration(PassProviderConfiguration("passprovider1", "/tmp"), TEST_CLOUD_CONFIGURATION_CONTEXT, TEST_LOG_CONTEXT).shouldBeTypeOf<Error<PassProviderRuntime>>()
        result.error shouldContain "before you may use the password store"
    }

    @Test
    fun testPassProviderInvalidDirSkipValidation() {
        System.setProperty("BLCKS_PASS_PROVIDER_SKIP_VALIDATION", "true")
        val result = provider.validateConfiguration(PassProviderConfiguration("passprovider1", "/tmp"), TEST_CLOUD_CONFIGURATION_CONTEXT, TEST_LOG_CONTEXT).shouldBeTypeOf<Success<PassProviderRuntime>>()
        result.data.passwordStoreDir shouldBe "/tmp"
        System.clearProperty("BLCKS_PASS_PROVIDER_SKIP_VALIDATION")
    }

    @Test
    fun testPassProviderNonExistentDir() {
        val result = provider.validateConfiguration(PassProviderConfiguration("passprovider1", "/foo-bar"), TEST_CLOUD_CONFIGURATION_CONTEXT, TEST_LOG_CONTEXT).shouldBeTypeOf<Error<PassProviderRuntime>>()
        result.error shouldBe "password store directory '/foo-bar' does not exist"
    }
}
