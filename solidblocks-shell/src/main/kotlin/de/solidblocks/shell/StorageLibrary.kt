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

  enum class FileSystem {
    ext4,
  }

  data class Mount(
      val device: String,
      val path: String,
      val filesystem: FileSystem = FileSystem.ext4,
  ) : LibraryCommand {
    override fun commands() = listOf("storage_mount $device $path ${filesystem.name}")
  }
}
