package de.solidblocks.cli

import de.solidblocks.cli.hetzner.resources.FloatingIpResponse
import de.solidblocks.cli.utils.pascalCaseToWhiteSpace
import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun pascalCaseToWhiteSpace() {
        assertEquals("floating ip", FloatingIpResponse::class.pascalCaseToWhiteSpace())
    }
}

