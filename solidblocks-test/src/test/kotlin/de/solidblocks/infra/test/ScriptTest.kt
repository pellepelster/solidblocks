import de.solidblocks.infra.test.output.stderrShouldMatch
import de.solidblocks.infra.test.output.stdoutShouldMatch
import de.solidblocks.infra.test.runLocal
import de.solidblocks.infra.test.script
import de.solidblocks.infra.test.shouldHaveExitCode
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test

public class ScriptTest {

    @Test
    fun testFunction() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        script().includes(include1)
            .step("hello_world") {
                it
            }.step("hello_universe") {
                it
            }
    }

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
    fun testScriptErrorUnboundVariable() {
        val include = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        val result = script().includes(include)
            .step("echo \${invalid}").runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 1
            it stderrShouldMatch  ".*invalid: unbound variable.*"
        }
    }
}