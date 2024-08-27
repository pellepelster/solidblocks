package de.solidblocks.shell.test

import de.solidblocks.infra.test.command.shouldHaveExitCode
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.infra.test.output.stdoutShouldMatch
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.Test
import testLocal
import java.util.UUID

public class SoftwareTest {

    @Test
    fun testEnsureRestic() {

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_restic")
            .step("software_set_export_path")
            .step("restic version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*restic 0.15.1.*"
        }
    }

    @Test
    fun testEnsureConsul() {

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_consul")
            .step("software_set_export_path")
            .step("consul version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Consul v1.12.3.*"
        }
    }

    @Test
    fun testEnsureConsulDifferentDirs() {

        val binDir = "/tmp/${UUID.randomUUID()}"
        val cacheDir = "/tmp/${UUID.randomUUID()}"

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .env("BIN_DIR" to binDir)
            .env("CACHE_DIR" to cacheDir)
            .step("software_ensure_consul")
            .step("software_set_export_path")
            .step("consul version") {
                it.fileExists("${binDir}/shellcheck-v0.8.0/shellcheck")
                it.fileExists("${cacheDir}/shellcheck-v0.8.0.linux.x86_64.tar.xz")
            }
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Consul v1.12.3.*"
        }
    }

    @Test
    fun testEnsureConsulDifferentVersions() {
        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_consul 1.8.4 0d74525ee101254f1cca436356e8aee51247d460b56fc2b4f7faef8a6853141f")
            .step("software_set_export_path")
            .step("consul version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Consul v1.8.4.*"
        }
    }

    @Test
    fun testEnsureShellcheck() {

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_shellcheck")
            .step("software_set_export_path")
            .step("shellcheck --version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*version: 0.8.0.*"
        }
    }

    @Test
    fun testEnsureHugo() {

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_hugo")
            .step("software_set_export_path")
            .step("hugo version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*hugo v0.101.0-466fa43c16709b4483689930a4f9ac8add5c9f66.*"
        }
    }

    @Test
    fun testEnsureTerragrunt() {

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_terragrunt")
            .step("software_set_export_path")
            .step("terragrunt --version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*terragrunt version v0.43.0.*"
        }
    }

    @Test
    fun testEnsureTerraform() {

        val result = testLocal().script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("software.sh"))
            .step("software_ensure_terraform")
            .step("software_set_export_path")
            .step("terraform version")
            .run()

        assertSoftly(result) {
            it shouldHaveExitCode 0
            it stdoutShouldMatch ".*Terraform v1.2.6.*"
        }
    }

}
