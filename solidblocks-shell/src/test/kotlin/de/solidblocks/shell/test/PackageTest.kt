package de.solidblocks.shell.test

import de.solidblocks.infra.test.script
import de.solidblocks.infra.test.shouldHaveExitCode
import de.solidblocks.infra.test.workingDir
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

public class PackageTest {

    @Test
    fun testEnsurePackage() {

        val result = script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("package.sh"))
            .step("package_update_repositories")
            .step("package_update_system") {
                it.fileExists("/usr/bin/wget") shouldBe false
            }
            .step("package_ensure_package wget") {
                it.fileExists("/usr/bin/wget") shouldBe true
            }
            .step("package_ensure_package wget") {
                it.fileExists("/usr/bin/wget") shouldBe true
            }
            .runDocker()

        assertSoftly(result) {
            it shouldHaveExitCode 0
        }
    }

}
