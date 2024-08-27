package de.solidblocks.infra.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.testDocker
import de.solidblocks.infra.test.output.stdoutShouldMatch
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import testLocal
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

public class ScriptTest {

    @Test
    fun testScriptLocal() {
        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        testLocal().script().includes(include1)
            .step("hello_world") {
                it.waitForOutput(".*hello world.*")
            }.step("hello_universe") {
                it.waitForOutput(".*hello universe.*")
            }.run()
    }

    @Test
    fun testScriptDocker() {
        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        testDocker(DockerTestImage.UBUNTU_22).script()
            .includes(include1)
            .step("hello_world") {
                it.waitForOutput(".*hello world.*")
            }.step("hello_universe") {
                it.waitForOutput(".*hello universe.*")
            }.run()
    }

    @Test
    fun testScriptRunLocal() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        val result = testLocal().script().includes(include1)
            .step("hello_world") {
                it.waitForOutput(".*hello world.*")
            }.step("hello_universe") {
                it.waitForOutput(".*hello universe.*")
            }.run()

        assertSoftly(result) {
            it stdoutShouldMatch ".*hello world.*"
            it stdoutShouldMatch ".*hello universe.*"
        }
    }

    @Test
    fun testScriptAssertFileLocal() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path
        val file = UUID.randomUUID()

        val result = testLocal().script().defaultWaitForOutput(5.seconds).includes(include1)
            .step("echo \"content\" > /tmp/${file}") {
                it.fileExists("/tmp/${file}") shouldBe true
                it.fileExists("/tmp/${file}_1") shouldBe false
            }.run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }

    @Test
    fun testScriptAssertFileDocker() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path
        val file = UUID.randomUUID()

        val result = testDocker(DockerTestImage.DEBIAN_10).script().includes(include1)
            .step("echo \"content\" > /tmp/${file}") {
                it.fileExists("/tmp/${file}") shouldBe true
                it.fileExists("/tmp/${file}_1") shouldBe false
            }.run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }


    @Test
    fun testScriptErrorUnboundVariable() {
        val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        val exception = shouldThrow<RuntimeException> {
            testLocal().script()
                .defaultWaitForOutput(5.seconds)
                .includes(include)
                .step("echo \${invalid}").run()

        }
        exception.message shouldBe ("timeout of 5s exceeded waiting for log line '.*finished step 0.*'")
    }

    @Test
    fun testScriptErrorUnboundVariableNoAsserts() {
        val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        testLocal().script()
            .assertSteps(false)
            .includes(include)
            .step("echo \${invalid}").run()

    }
}
