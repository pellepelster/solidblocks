package de.solidblocks.shell

import java.io.BufferedReader

object StorageLibrary {
    // TODO remove storage.sh from cloud-init
    fun source() =
        StorageLibrary::class.java.classLoader.getResourceAsStream("storage.sh").bufferedReader(Charsets.UTF_8).use(
            BufferedReader::readText
        )

    enum class Filesystem {
        ext4
    }

    data class Mount(val storageDevice: String, val storageDir: String, val filesystem: Filesystem = Filesystem.ext4) :
        LibraryCommand {
        override fun toShell() = listOf("storage_mount ${storageDevice} ${storageDir} ${filesystem.name}")
    }

    data class MkDir(val dir: String) :
        LibraryCommand {
        override fun toShell() = listOf("mkdir -p ${dir}")
    }
}
