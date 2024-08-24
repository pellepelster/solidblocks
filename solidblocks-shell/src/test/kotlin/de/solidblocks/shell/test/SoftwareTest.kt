import de.solidblocks.infra.test.output.stdoutShouldMatch
import de.solidblocks.infra.test.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.workingDir
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

public class SoftwareTest {

    private fun getCommandPath(path: String) = Path(this.javaClass.classLoader.getResource(path)!!.path)

    @Test
    fun testEnsureRestic() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_restic")
            .step("software_set_export_path")
            .step("restic version")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*restic 0.15.1.*"
        }
    }

    @Test
    fun testEnsureConsul() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_consul")
            .step("software_set_export_path")
            .step("consul version")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Consul v1.12.3.*"
        }
    }

    @Test
    fun testEnsureShellcheck() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_shellcheck")
            .step("software_set_export_path")
            .step("shellcheck --version")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*version: 0.8.0.*"
        }
    }

    @Test
    fun testEnsureHugo() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_hugo")
            .step("software_set_export_path")
            .step("hugo version")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*hugo v0.101.0-466fa43c16709b4483689930a4f9ac8add5c9f66.*"
        }
    }

    @Test
    fun testEnsureTerragrunt() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_terragrunt")
            .step("software_set_export_path")
            .step("terragrunt --version")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*terragrunt version v0.43.0.*"
        }
    }

    @Test
    fun testEnsureTerraform() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_terraform")
            .step("software_set_export_path")
            .step("terraform version")
            .runLocal()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Terraform v1.2.6.*"
        }
    }

}
