package de.solidblocks.cloudinit.model

enum class Filesystem {
    ext4
}

data class Mount(val storageDevice: String, val storageDir: String, val filesystem: Filesystem = Filesystem.ext4)

data class CloudInit(var environmentVariables: Map<String, String> = mutableMapOf()) {
    val mounts = ArrayList<Mount>()

}