package de.solidblocks.vault

import de.solidblocks.test.SolidblocksLocalEnv
import de.solidblocks.test.SolidblocksLocalEnvExtension
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.ExperimentalTime

data class TestData(val string: String, val number: Number, val boolean: Boolean)

@ExtendWith(SolidblocksLocalEnvExtension::class)
class VaultManagerTest {

    private val logger = KotlinLogging.logger {}

    @OptIn(ExperimentalTime::class)
    @Test
    fun testStoreAndLoadData(solidblocksLocalEnv: SolidblocksLocalEnv) {
        logger.info { "local env vault at '${solidblocksLocalEnv.vaultAddress}' with root token '${solidblocksLocalEnv.rootToken}'" }

        val vaultManager = VaultManager(
            address = solidblocksLocalEnv.vaultAddress,
            _token = solidblocksLocalEnv.rootToken,
            solidblocksLocalEnv.reference
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
