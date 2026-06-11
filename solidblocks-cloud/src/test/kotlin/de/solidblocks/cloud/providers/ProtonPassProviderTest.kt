package de.solidblocks.cloud.providers

import de.solidblocks.cloud.TEST_CLOUD_CONFIGURATION_CONTEXT
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.providers.protonpass.ProtonPassProviderConfiguration
import de.solidblocks.cloud.providers.protonpass.ProtonPassProviderManager
import de.solidblocks.cloud.providers.protonpass.ProtonPassProviderRuntime
import de.solidblocks.cloud.utils.Success
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

@DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
class ProtonPassProviderTest {

    val provider = ProtonPassProviderManager()

    @Test
    fun `proton pass provider defaults to cloud name`() {
        val result = provider.validateConfiguration(
            ProtonPassProviderConfiguration("protonpassprovider1", null),
            TEST_CLOUD_CONFIGURATION_CONTEXT,
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Success<ProtonPassProviderRuntime>>()
        result.data.vaultName shouldBe "cloud1-default"
    }

    @Test
    fun `proton pass provider uses configured vault name`() {
        val result = provider.validateConfiguration(
            ProtonPassProviderConfiguration("protonpassprovider1", "proton-passprovider-test"),
            TEST_CLOUD_CONFIGURATION_CONTEXT,
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Success<ProtonPassProviderRuntime>>()
        result.data.vaultName shouldBe "proton-passprovider-test"
    }
}
