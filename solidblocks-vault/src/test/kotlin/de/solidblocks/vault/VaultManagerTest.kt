package de.solidblocks.vault

import de.solidblocks.test.IntegrationTestEnvironment
import de.solidblocks.test.IntegrationTestExtension
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.ExperimentalTime

data class TestData(val string: String, val number: Number, val boolean: Boolean)

@ExtendWith(IntegrationTestExtension::class)
class VaultManagerTest {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalTime::class)
    @Test
    fun testStoreAndLoadData(integrationTestEnvironment: IntegrationTestEnvironment) {
        logger.info { "local env vault at '${integrationTestEnvironment.vaultAddress}' with root token '${integrationTestEnvironment.vaultRootToken}'" }

        val vaultManager = EnvironmentVaultManager(
            integrationTestEnvironment.vaultAddress,
            integrationTestEnvironment.vaultRootToken,
            integrationTestEnvironment.reference
        )

        assertThat(vaultManager.hasKv("test")).isFalse
        assertThat(vaultManager.storeKv("test", TestData("string1", 123, true))).isTrue
        assertThat(vaultManager.hasKv("test")).isTrue

        val testData = vaultManager.loadKv("test", TestData::class.java)

        assertThat(testData!!.string).isEqualTo("string1")
        assertThat(testData.number).isEqualTo(123)
        assertThat(testData.boolean).isTrue
    }
}
