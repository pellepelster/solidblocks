package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.ResticLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class ResticTest {

  @Test
  fun testLibrarySource() {
    ResticLibrary.source() shouldContain "restic_ensure_repo"
  }

  @Test
  fun testFlow() {
    val data = UUID.randomUUID().toString()

    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("curl.sh"))
            .includes(workingDir().resolve("lib").resolve("restic.sh"))
            .step("restic_install")
            .step("restic_ensure_repo /tmp/repo1 password1")
            .step("restic_ensure_repo /tmp/repo1 password1")
            .step("restic_restore /tmp/repo1 password1")
            .step("mkdir -p /backup1")
            .step("echo $data > /backup1/file.txt")
            .step("restic_backup /tmp/repo1 password1 /backup1")
            .step("restic_stats /tmp/repo1 password1")
            .step("restic_snapshots /tmp/repo1 password1")
            .step("rm -rf /backup1")
            .step("mkdir -p /backup1")
            .step("restic_restore /tmp/repo1 password1") {
              it.fileExists("/backup1/file.txt") shouldBe true
              it.fileContent("/backup1/file.txt") shouldBe data
            }
            .step("ls -lsa /backup1")
            .run()
    assertSoftly(result) { it shouldHaveExitCode 0 }
  }
}
