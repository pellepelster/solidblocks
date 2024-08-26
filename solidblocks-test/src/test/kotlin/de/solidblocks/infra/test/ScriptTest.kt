package de.solidblocks.infra.test

import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.output.stdoutShouldMatch
import de.solidblocks.infra.test.script.script
import de.solidblocks.infra.test.shouldHaveExitCode
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

public class ScriptTest {

    @Test
    fun testScriptRunDocker() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        script().includes(include1)
            .step("hello_world") {
                it
            }.step("hello_universe") {
                it
            }.runLocal()
    }

    @Test
    fun testScriptRunLocal() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        val result = script().includes(include1)
            .step("hello_world") {
                it.waitForOutput(".*hello world.*")
            }.step("hello_universe") {
                it.waitForOutput(".*hello universe.*")
            }.runLocal()

        assertSoftly(result) {
            it stdoutShouldMatch ".*hello world.*"
            it stdoutShouldMatch ".*hello universe.*"
        }
    }

    @Test
    fun testScriptAssertFileLocal() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path
        val file = UUID.randomUUID()

        val result = script().defaultWaitForOutput(5.seconds).includes(include1)
            .step("echo \"content\" > /tmp/${file}") {
                it.fileExists("/tmp/${file}") shouldBe true
                it.fileExists("/tmp/${file}_1") shouldBe false
            }.runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }

    @Test
    fun testScriptAssertFileDocker() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path
        val file = UUID.randomUUID()

        val result = script().includes(include1)
            .step("echo \"content\" > /tmp/${file}") {
                it.fileExists("/tmp/${file}") shouldBe true
                it.fileExists("/tmp/${file}_1") shouldBe false
            }.runDocker(DockerTestImage.DEBIAN_10)

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }


    @Test
    fun testScriptErrorUnboundVariable() {
        val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        val exception = shouldThrow<RuntimeException> {
            script()
                .defaultWaitForOutput(5.seconds)
                .includes(include)
                .step("echo \${invalid}").runLocal()

        }
        exception.message shouldBe ("timeout of 5s exceeded waiting for log line '.*finished step 0.*'")
    }

    @Test
    fun testScriptErrorUnboundVariableNoAsserts() {
        val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        script()
            .assertSteps(false)
            .includes(include)
            .step("echo \${invalid}").runLocal()

    }
}
