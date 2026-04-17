package de.solidblocks.shell.test

import de.solidblocks.infra.test.assertions.shouldHaveExitCode
import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.dockerTestContext
import de.solidblocks.infra.test.files.workingDir
import de.solidblocks.shell.CaddyLibrary
import de.solidblocks.shell.caddy.CaddyConfig
import de.solidblocks.shell.caddy.FileSystemStorage
import de.solidblocks.shell.caddy.GlobalOptions
import de.solidblocks.shell.caddy.ReverseProxy
import de.solidblocks.shell.caddy.Site
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

public class CaddyTest {
    @Test
    fun testEnsurePackage() {
        val result =
            dockerTestContext(DockerTestImage.DEBIAN_12)
                .script()
                .sources(workingDir().resolve("lib"))
                .includes(workingDir().resolve("lib").resolve("curl.sh"))
                .includes(workingDir().resolve("lib").resolve("caddy.sh"))
                .step("caddy_install") { it.fileExists("/usr/bin/caddy") shouldBe true }
                .run()

        assertSoftly(result) { it shouldHaveExitCode 0 }
    }

    @Test
    fun testLibrarySource() {
        CaddyLibrary.source() shouldContain "caddy_install"
    }

    @Test
    fun testRender() {
        val config =
            CaddyConfig(
                GlobalOptions(FileSystemStorage("/data/storage/www"), "info@yolo.de"),
                listOf(Site("yolo.de", ReverseProxy("http://localhost:3903"))),
            )

        config.render() shouldBe
            """
        {
          storage file_system {
            root /data/storage/www
          }
          email info@yolo.de
        }

        yolo.de {
          reverse_proxy http://localhost:3903
        }

        """
                .trimIndent()
    }
}
