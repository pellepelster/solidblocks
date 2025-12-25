package de.solidblocks.infra.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.assertions.stdoutShouldMatch
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.util.*
import kotlin.time.Duration.Companion.seconds
import localTestContext
import org.junit.jupiter.api.Test

public class ScriptTest {

  @Test
  fun testScriptLocal() {
    val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

    localTestContext()
        .script()
        .includes(include1)
        .step("hello_world") { it.waitForOutput(".*hello world.*") }
        .step("hello_universe") { it.waitForOutput(".*hello universe.*") }
        .run()
  }

  @Test
  fun testScriptDocker() {
    val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

    dockerTestContext(DockerTestImage.UBUNTU_22)
        .script()
        .includes(include1)
        .step("hello_world") { it.waitForOutput(".*hello world.*") }
        .step("hello_universe") { it.waitForOutput(".*hello universe.*") }
        .run()
  }

  @Test
  fun testScriptRunLocal() {
    val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

    val result =
        localTestContext()
            .script()
            .includes(include1)
            .step("hello_world") { it.waitForOutput(".*hello world.*") }
            .step("hello_universe") { it.waitForOutput(".*hello universe.*") }
            .run()

    assertSoftly(result) {
      it stdoutShouldMatch ".*hello world.*"
      it stdoutShouldMatch ".*hello universe.*"
    }
  }

  @Test
  fun testScriptAssertFileLocal() {
    val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path
    val file = UUID.randomUUID()

    val result =
        localTestContext()
            .script()
            .defaultWaitForOutput(5.seconds)
            .includes(include1)
            .step("echo \"content\" > /tmp/$file") {
              it.fileExists("/tmp/$file") shouldBe true
              it.fileExists("/tmp/${file}_1") shouldBe false
            }
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }

  @Test
  fun testScriptAssertFileDocker() {
    val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path
    val file = UUID.randomUUID()

    val result =
        dockerTestContext(DockerTestImage.DEBIAN_10)
            .script()
            .includes(include1)
            .step("echo \"content\" > /tmp/$file") {
              it.fileExists("/tmp/$file") shouldBe true
              it.fileExists("/tmp/${file}_1") shouldBe false
            }
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }

  @Test
  fun testScriptErrorUnboundVariable() {
    val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

    val exception =
        shouldThrow<RuntimeException> {
          localTestContext()
              .script()
              .defaultWaitForOutput(5.seconds)
              .includes(include)
              .step("echo \${invalid}")
              .run()
        }
    exception.message shouldBe ("timeout of 5s exceeded waiting for log line '.*finished step 0.*'")
  }

  @Test
  fun testScriptErrorUnboundVariableNoAsserts() {
    val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

    localTestContext().script().assertSteps(false).includes(include).step("echo \${invalid}").run()
  }
}
