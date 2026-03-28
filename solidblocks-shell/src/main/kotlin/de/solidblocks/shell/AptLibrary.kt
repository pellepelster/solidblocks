package de.solidblocks.shell

import java.io.BufferedReader

object AptLibrary {
  fun source() =
      AptLibrary::class
          .java
          .classLoader
          .getResourceAsStream("apt.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(
              BufferedReader::readText,
          )

  class UpdateRepositories : LibraryCommand {
    override fun commands() = listOf("apt_update_repositories")
  }
}
