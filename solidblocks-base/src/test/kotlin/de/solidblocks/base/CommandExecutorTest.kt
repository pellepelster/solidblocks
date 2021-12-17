package de.solidblocks.base

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommandExecutorTest {

    @Test
    fun testSuccess() {
        val result = CommandExecutor().run(command = listOf("whoami"))
        assertThat(result.stdout).isEqualTo(System.getProperty("user.name"))
        assertThat(result.stderr).isEmpty()
        assertThat(result.stderr).isEmpty()
        assertThat(result.error).isFalse
    }

    @Test
    fun testError() {
        val result = CommandExecutor().run(command = listOf("whoami", "invalid"))
        assertThat(result.stdout).isEqualTo("")
        assertThat(result.stderr).isEqualTo(
            "whoami: extra operand ‘invalid’\n" +
                "Try 'whoami --help' for more information."
        )
        assertThat(result.error).isTrue
    }

    @Test
    fun testEnvironment() {
        val result = CommandExecutor().run(environment = mapOf("XXX" to "YYY"), command = listOf("env"))
        assertThat(result.stdout).contains("XXX=YYY")
        assertThat(result.stderr).isEmpty()
        assertThat(result.error).isFalse
    }
}
