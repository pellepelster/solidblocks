package de.solidblocks.cloudinit.model

enum class Filesystem {
    ext4
}

data class Mount(val storageDevice: String, val storageDir: String, val filesystem: Filesystem = Filesystem.ext4) :
    CloudInitScriptCommand {
    override fun toShell() = listOf("storage_mount ${storageDevice} ${storageDir} ${filesystem.name}")
}
