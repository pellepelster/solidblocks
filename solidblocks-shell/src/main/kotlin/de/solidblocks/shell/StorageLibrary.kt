package de.solidblocks.shell

object StorageLibrary : ShellLibrary {
    override fun name() = "storage"

    enum class FileSystem {
        ext4,
    }

    data class Mount(val device: String, val path: String, val filesystem: FileSystem = FileSystem.ext4) : LibraryCommand {
        override fun commands() = listOf("storage_mount $device $path ${filesystem.name}")
    }
}
