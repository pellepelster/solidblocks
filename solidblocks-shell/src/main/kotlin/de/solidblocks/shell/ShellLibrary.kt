package de.solidblocks.shell

import java.io.BufferedReader

interface ShellLibrary {
  fun source() =
      ShellLibrary::class
          .java
          .classLoader
          .getResourceAsStream("${name()}.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(BufferedReader::readText)

  fun name(): String
}
