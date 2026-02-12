package de.solidblocks.shell

import java.io.BufferedReader

object StorageLibrary {
  // TODO remove storage.sh from cloud-init
  fun source() =
      StorageLibrary::class
          .java
          .classLoader
          .getResourceAsStream("storage.sh")
          .bufferedReader(Charsets.UTF_8)
          .use(BufferedReader::readText)

  @Suppress("ktlint:standard:enum-entry-name-case")
  enum class FileSystem {
    ext4,
  }

  data class Mount(
      val storageDevice: String,
      val storageDir: String,
      val filesystem: FileSystem = FileSystem.ext4,
  ) : LibraryCommand {
    override fun toShell() = listOf("storage_mount $storageDevice $storageDir ${filesystem.name}")
  }

  data class MkDir(val dir: String, val owner: String? = null) : LibraryCommand {
    override fun toShell() =
        listOf("mkdir -p $dir") + if (owner != null) listOf("chown $owner $dir") else emptyList()
  }
}
