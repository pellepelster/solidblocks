package de.solidblocks.infra.test

import de.solidblocks.infra.test.docker.DockerTestImage
import de.solidblocks.infra.test.docker.testDocker
import de.solidblocks.infra.test.files.tempDir
import java.io.Closeable
import testLocal

class SolidblocksTestContext {

  private val tempDirs = mutableListOf<Closeable>()

  fun createTempDir() = tempDir().apply { tempDirs.add(this) }

  fun local() = testLocal().apply { tempDirs.add(this) }

  fun docker(image: DockerTestImage) = testDocker(image).apply { tempDirs.add(this) }

  fun close() {
    tempDirs.forEach { it.close() }
  }
}
