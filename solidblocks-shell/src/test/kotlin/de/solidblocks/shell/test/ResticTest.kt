package de.solidblocks.shell.test

import de.solidblocks.infra.test.SolidblocksTest
import de.solidblocks.infra.test.SolidblocksTestContext
import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.ResticLibrary
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.System.getenv
import java.util.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SolidblocksTest::class)
public class ResticTest {

  @Test
  fun testLibrarySource() {
    ResticLibrary.source() shouldContain "restic_ensure_local_repo"
  }

  @Test
  fun testLocalBackupRestore() {
    val data = UUID.randomUUID().toString()

    val localRepository = "/tmp/backup/repo1"
    val repositoryPassword = "password1"

    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("curl.sh"))
            .includes(workingDir().resolve("lib").resolve("restic.sh"))
            .step("restic_install")
            .step(ResticLibrary.WriteCredentials(repositoryPassword).toShell())
            .step(ResticLibrary.EnsureLocalRepo(localRepository).toShell())
            .step(ResticLibrary.EnsureLocalRepo(localRepository).toShell())
            .step("restic_restore $localRepository $repositoryPassword")
            .step("mkdir -p /backup1")
            .step("echo $data > /backup1/file.txt")
            .step(ResticLibrary.Backup(localRepository, "backup1").toShell())
            .step("restic_stats $localRepository $repositoryPassword")
            .step("restic_snapshots $localRepository $repositoryPassword --latest 1")
            .step("rm -rf /backup1")
            .step("mkdir -p /backup1")
            .step(ResticLibrary.Restore(localRepository).toShell()) {
              it.fileExists("/backup1/file.txt") shouldBe true
              it.fileContent("/backup1/file.txt") shouldBe data
            }
            .step(ResticLibrary.Restore(localRepository).toShell()) {
              it.waitForOutput(".*is not empty, canceling restore.*")
            }
            .step("ls -lsa /backup1")
            .run()
    assertSoftly(result) { it shouldHaveExitCode 0 }
  }

  @Test
  fun testS3BackupRestore(testContext: SolidblocksTestContext) {
    val bucket = testContext.aws().createBucket()

    val data = UUID.randomUUID().toString()

    val s3Repository = "s3:s3.eu-central-1.amazonaws.com/$bucket/repo1"
    val repositoryPassword = "password1"

    val result =
        dockerTestContext(DockerTestImage.DEBIAN_12)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("curl.sh"))
            .includes(workingDir().resolve("lib").resolve("restic.sh"))
            .step("restic_install")
            .step(
                ResticLibrary.WriteS3Credentials(
                        repositoryPassword,
                        getenv("AWS_ACCESS_KEY_ID"),
                        getenv("AWS_SECRET_ACCESS_KEY"),
                    )
                    .toShell(),
            )
            .step(ResticLibrary.EnsureS3Repo(s3Repository).toShell())
            .step(ResticLibrary.EnsureS3Repo(s3Repository).toShell())
            .step("restic_restore $s3Repository")
            .step("mkdir -p /backup1")
            .step("echo $data > /backup1/file.txt")
            .step(ResticLibrary.Backup(s3Repository, "backup1").toShell())
            .step("restic_stats $s3Repository")
            .step("restic_snapshots $s3Repository")
            .step("rm -rf /backup1")
            .step("mkdir -p /backup1")
            .step(ResticLibrary.Restore(s3Repository).toShell()) {
              it.fileExists("/backup1/file.txt") shouldBe true
              it.fileContent("/backup1/file.txt") shouldBe data
            }
            .step("ls -lsa /backup1")
            .run()
    assertSoftly(result) { it shouldHaveExitCode 0 }
  }
}
