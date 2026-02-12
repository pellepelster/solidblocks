package de.solidblocks.shell

import java.io.BufferedReader

object CaddyLibrary {
  fun source() =
      CaddyLibrary::class
          .java
          .classLoader
          .getResourceAsStream("caddy.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(
              BufferedReader::readText,
          )

  class Install : LibraryCommand {
    override fun toShell() = listOf("caddy_install")
  }
}
