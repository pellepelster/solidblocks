import de.solidblocks.infra.test.runLocal
import de.solidblocks.infra.test.script
import org.junit.jupiter.api.Test

public class ScriptTest {

    @Test
    fun testFunction() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        script().sources(include1)
            .step("hello_world") {
                it
            }.step("hello_universe") {
                it
            }
    }

    @Test
    fun testRunScriptLocal() {

        val include1 = this.javaClass.classLoader.getResource("script-include1.sh")!!.path

        script().sources(include1)
            .step("hello_world") {
                it
            }.step("hello_universe") {
                it
            }.runLocal()
    }
}