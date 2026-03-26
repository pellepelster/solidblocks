package de.solidblocks.shell

import java.io.BufferedReader

object ResticLibrary {
  fun source() =
      ResticLibrary::class
          .java
          .classLoader
          .getResourceAsStream("restic.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(
              BufferedReader::readText,
          )

  class Install : LibraryCommand {
    override fun toShell() = listOf("restic_install")
  }

  class EnsureRepo(val repository: String, val password: String) : LibraryCommand {
    override fun toShell() = listOf("restic_ensure_repo '$repository' '$password'")
  }

  class Backup(val repository: String, val password: String, val directory: String) :
      LibraryCommand {
    override fun toShell() = listOf("restic_backup '$repository' '$password' '$directory'")
  }

  class Restore(val repository: String, val password: String) : LibraryCommand {
    override fun toShell() = listOf("restic_restore '$repository' '$password'")
  }
}
