package de.solidblocks.cli

import de.solidblocks.cloud.utils.commandExists
import de.solidblocks.cloud.utils.runCommand
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.util.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UtilsTest {
  @Test
  fun testCommandExists() {
    commandExists("whoami") shouldBe true
    commandExists("invalid") shouldBe false
  }

  @Test
  fun testCommand() {
    assertSoftly(runCommand(listOf("whoami"))) {
      it?.exitCode shouldBe 0
      it?.stdout shouldBe "pelle\n"
      it?.stderr shouldBe ""
    }
  }

  @Test
  fun testInvalid() {
    runCommand(listOf("invalid")) shouldBe null
  }

  @Test
  @Disabled
  fun testPassShow() {
    println(runCommand(listOf("pass", "test"))?.stdout)
  }

  @Test
  @Disabled
  fun testPassInsert() {
    val random = UUID.randomUUID().toString()
    runCommand(listOf("pass", "insert", "--multiline", "--force", "test"), random)
    runCommand(listOf("pass", "test"))?.stdout shouldBe random
  }
}
