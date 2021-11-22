package de.solidblocks.cli

import org.apache.commons.net.util.SubnetUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UtilsTest {
    @Test
    fun testCompareVaultTtl() {
        assertEquals("10.0.1.0/24", SubnetUtils("10.0.1.0/24").info.cidrSignature)
    }
}
