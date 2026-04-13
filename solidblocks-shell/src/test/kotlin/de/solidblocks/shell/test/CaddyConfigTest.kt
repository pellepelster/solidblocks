package de.solidblocks.shell.test

import de.solidblocks.shell.caddy.CaddyConfig
import de.solidblocks.shell.caddy.FileSystemStorage
import de.solidblocks.shell.caddy.GlobalOptions
import de.solidblocks.shell.caddy.ReverseProxy
import de.solidblocks.shell.caddy.Site
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CaddyConfigTest {
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
