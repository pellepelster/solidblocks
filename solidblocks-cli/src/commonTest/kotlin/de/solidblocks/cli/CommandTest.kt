package de.solidblocks.cli

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommandTest {

    @Test
    fun testCommandExists() {
        assertTrue(commandExists("ping"))
        assertFalse(commandExists("pong"))
    }

}