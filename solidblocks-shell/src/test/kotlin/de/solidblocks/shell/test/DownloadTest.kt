package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.io.File
import java.util.UUID
import org.junit.jupiter.api.Test

public class DownloadTest {
  @Test
  fun testDownloadAndVerifyChecksum() {
    val filename = UUID.randomUUID().toString()

    val result =
        dockerTestContext(DockerTestImage.DEBIAN_10)
            .script()
            .sources(workingDir().resolve("lib"))
            .includes(workingDir().resolve("lib").resolve("download.sh"))
            .step(
                "download_and_verify_checksum \"https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip\" /tmp/$filename.zip \"dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253\"",
            ) {
              it.fileExists("/tmp/$filename.zip") shouldBe true
              it.sha256sum("/tmp/$filename.zip") shouldBe
                  "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
              File("/tmp/$filename.zip").writeText("invalid")
            }
            .step(
                "download_and_verify_checksum \"https://releases.hashicorp.com/nomad/0.12.5/nomad_0.12.5_linux_amd64.zip\" /tmp/$filename.zip \"dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253\"",
            ) {
              it.fileExists("/tmp/$filename.zip") shouldBe true
              it.sha256sum("/tmp/$filename.zip") shouldBe
                  "dece264c86a5898a18d62d6ecca469fee71329e444b284416c57bd1e3d76f253"
            }
            .run()

    assertSoftly(result) { it shouldHaveExitCode 0 }
  }
}
