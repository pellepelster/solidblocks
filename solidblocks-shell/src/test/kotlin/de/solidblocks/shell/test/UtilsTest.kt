package de.solidblocks.shell.test

import de.solidblocks.shell.UtilsLibrary
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class UtilsTest {

    @Test
    fun testLibrarySource() {
        UtilsLibrary.source() shouldContain "ensure_command"
    }
}

