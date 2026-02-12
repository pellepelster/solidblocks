package de.solidblocks.shell

import java.io.BufferedReader

object CurlLibrary {
  fun source() =
      CurlLibrary::class
          .java
          .classLoader
          .getResourceAsStream("curl.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(
              BufferedReader::readText,
          )
}
