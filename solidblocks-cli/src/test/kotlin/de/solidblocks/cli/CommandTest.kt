package de.solidblocks.cli

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CommandTest {

    @Test
    fun testCommandExists() {
        assertTrue(commandExists("ping"))
        assertFalse(commandExists("pong"))
    }

}