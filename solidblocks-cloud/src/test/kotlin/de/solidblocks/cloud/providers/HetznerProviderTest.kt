package de.solidblocks.cloud.providers

import de.solidblocks.cloud.TEST_CLOUD_CONFIGURATION_CONTEXT
import de.solidblocks.cloud.TEST_LOG_CONTEXT
import de.solidblocks.cloud.providers.hetzner.HetznerProviderConfiguration
import de.solidblocks.cloud.providers.hetzner.HetznerProviderManager
import de.solidblocks.cloud.providers.hetzner.HetznerProviderRuntime
import de.solidblocks.cloud.providers.pass.PassProviderConfiguration
import de.solidblocks.cloud.providers.pass.PassProviderManager
import de.solidblocks.cloud.providers.pass.PassProviderRuntime
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Success
import de.solidblocks.hetzner.cloud.model.HetznerLocation
import de.solidblocks.hetzner.cloud.model.HetznerServerType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable

class HetznerProviderTest {

    val provider = HetznerProviderManager()

    @Test
    fun testInvalidCloudToken() {
        System.setProperty("HCLOUD_TOKEN", "invalid_token")

        val result = provider.validateConfiguration(
            HetznerProviderConfiguration("hetznerprovider1", HetznerLocation.nbg1, HetznerServerType.cx23),
            TEST_CLOUD_CONFIGURATION_CONTEXT,
            TEST_LOG_CONTEXT,
        ).shouldBeTypeOf<Error<HetznerProviderRuntime>>()
        result.error shouldContain "provided Hetzner cloud token is not valid"
    }
}
