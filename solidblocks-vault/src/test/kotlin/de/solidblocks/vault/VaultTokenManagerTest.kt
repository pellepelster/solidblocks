package de.solidblocks.vault

import de.solidblocks.test.IntegrationTestEnvironment
import de.solidblocks.test.IntegrationTestExtension
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilNotNull
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Duration.*

@ExtendWith(IntegrationTestExtension::class)
class VaultTokenManagerTest {

    private val logger = KotlinLogging.logger {}

    @Test
    fun testRenewToken(integrationTestEnvironment: IntegrationTestEnvironment) {

        val serviceToken = integrationTestEnvironment.createService("test")

        val vaultTokenManager = VaultTokenManager(
            address = integrationTestEnvironment.vaultAddress,
            token = serviceToken,
            checkInterval = ofSeconds(1),
            renewAtTtl = ofDays(30)
        )

        val initialTokenInfo = await untilNotNull { vaultTokenManager.tokenInfo }
        assertThat(initialTokenInfo.ttl).isGreaterThan(ofHours(24))

        val tokenInfo = await atMost(ofSeconds(40)) withPollDelay(ofSeconds(30)) untilNotNull { vaultTokenManager.tokenInfo }
        assertThat(initialTokenInfo.ttl).isCloseTo(tokenInfo.ttl, ofSeconds(2))
    }
}
