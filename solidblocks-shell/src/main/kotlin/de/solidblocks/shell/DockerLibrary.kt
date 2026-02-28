package de.solidblocks.shell

import java.io.BufferedReader

object DockerLibrary {
  fun source() =
      DockerLibrary::class
          .java
          .classLoader
          .getResourceAsStream("docker.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(
              BufferedReader::readText,
          )

  class InstallDebian : LibraryCommand {
    override fun toShell() = listOf("docker_install_debian")
  }
}
