package de.solidblocks.cloud

import de.solidblocks.cloud.CloudUtils.extractRootDomain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CloudUtilsTest {

    @Test
    fun testExtractRootDomain() {
        assertThat(extractRootDomain(null)).isNull()
        assertThat(extractRootDomain("")).isNull()
        assertThat(extractRootDomain(" ")).isNull()
        assertThat(extractRootDomain("    ")).isNull()

        assertThat(extractRootDomain("hostname1")).isEqualTo("hostname1")
        assertThat(extractRootDomain("http://hostname1")).isEqualTo("hostname1")
        assertThat(extractRootDomain("http://hostname1:8080")).isEqualTo("hostname1")
        assertThat(extractRootDomain("hostname1.local")).isEqualTo("hostname1.local")
        assertThat(extractRootDomain("http://hostname1.local")).isEqualTo("hostname1.local")
        assertThat(extractRootDomain("http://hostname1.local:8080")).isEqualTo("hostname1.local")
    }
}
