package de.solidblocks.cloud

import de.solidblocks.cloud.NetworkUtils.nextNetwork
import de.solidblocks.cloud.NetworkUtils.solidblocksNetwork
import de.solidblocks.cloud.NetworkUtils.subnetForNetwork
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NetworkUtilsTest {

    @Test
    fun tesSolidblocksNetwork() {
        assertThat(solidblocksNetwork()).isEqualTo("10.0.0.0/16")
    }

    @Test
    fun testNextNetwork() {
        assertThat(nextNetwork()).isEqualTo("10.1.0.0/16")
        assertThat(nextNetwork(setOf("10.1.0.0/16"))).isEqualTo("10.2.0.0/16")
    }

    @Test
    fun testSubnetForNetwork() {
        assertThat(subnetForNetwork("10.1.0.0/16")).isEqualTo("10.1.0.0/24")
    }
}
