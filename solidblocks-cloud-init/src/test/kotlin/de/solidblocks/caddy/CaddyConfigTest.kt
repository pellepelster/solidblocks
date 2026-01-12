package de.solidblocks.caddy

import de.solidblocks.systemd.Service
import de.solidblocks.systemd.SystemdConfig
import de.solidblocks.systemd.Unit
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CaddyConfigTest {

    @Test
    fun testRender() {
        val config = CaddyConfig(GlobalOptions(FileSystemStorage("/data/storage/www"), "info@yolo.de"), listOf(Site("yolo.de")))

        config.render() shouldBe """
        {
          storage file_system {
            root /data/storage/www
          }
          email info@yolo.de
        }
        
        yolo.de {
        }

        """.trimIndent()
    }
}