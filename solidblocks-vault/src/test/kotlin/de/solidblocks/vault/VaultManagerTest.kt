package de.solidblocks.vault

import de.solidblocks.test.DevelopmentEnvironment
import de.solidblocks.test.DevelopmentEnvironmentExtension
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.ExperimentalTime

data class TestData(val string: String, val number: Number, val boolean: Boolean)

@ExtendWith(DevelopmentEnvironmentExtension::class)
class VaultManagerTest {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalTime::class)
    @Test
    fun testStoreAndLoadData(developmentEnvironment: DevelopmentEnvironment) {
        logger.info { "local env vault at '${developmentEnvironment.vaultAddress}' with root token '${developmentEnvironment.rootToken}'" }

        val vaultManager = VaultManager(
            developmentEnvironment.vaultAddress,
            developmentEnvironment.rootToken,
            developmentEnvironment.reference
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
