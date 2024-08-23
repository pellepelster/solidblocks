import de.solidblocks.infra.test.runLocal
import de.solidblocks.infra.test.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

public class PackageTest {

    private fun getCommandPath(path: String) = Path(this.javaClass.classLoader.getResource(path)!!.path)

    @Test
    @Disabled
    fun testEnsurePackage() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("package.sh"))
            .step("package_update_repositories")
            .step("package_update_system")
            .step("package_ensure_package wget")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }

}
