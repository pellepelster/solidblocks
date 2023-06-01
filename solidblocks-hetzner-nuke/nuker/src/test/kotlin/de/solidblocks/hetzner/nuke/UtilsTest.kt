package de.solidblocks.hetzner.nuke

import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertFalse

class UtilsTest {
    @Test
    fun testValidLabelParameter() {
        assertTrue(validLabelParameter("a=b"))
        assertTrue(validLabelParameter("a=b"))
        assertFalse(validLabelParameter(""))
        assertFalse(validLabelParameter("a"))
        assertFalse(validLabelParameter("a="))
        assertFalse(validLabelParameter("=b"))
        assertFalse(validLabelParameter("="))
    }
}
