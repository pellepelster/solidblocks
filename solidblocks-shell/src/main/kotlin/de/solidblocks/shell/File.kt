package de.solidblocks.shell

import kotlin.io.encoding.Base64

class WriteFile(val content: ByteArray, val path: String, val permissions: FilePermissions = FilePermissions()) : LibraryCommand {
    override fun commands() = listOf(
        "touch $path",
        "chmod ${permissions.renderChmod()} $path",
        "echo \"${Base64.encode(content)}\" | base64 -d > $path",
    )
}

data class MkDir(val dir: String, val owner: String? = null, val group: String? = null) : LibraryCommand {
    override fun commands() = listOf("mkdir -p $dir") +
        (if (owner != null) listOf("chown $owner $dir") else emptyList()) +
        (if (group != null) listOf("chgrp $group $dir") else emptyList())
}

sealed class Permission(val read: Boolean, val write: Boolean, val execute: Boolean) {
    fun renderChmod() = "${if (read) "r" else "-"}${if (write) "w" else "-"}${if (execute) "x" else "-"}"
}

class UserPermission(read: Boolean = true, write: Boolean = true, execute: Boolean = false) : Permission(read, write, execute) {
    companion object {
        val RWX = UserPermission(true, true, true)
        val RW_ = UserPermission(true, true, false)
        val R__ = UserPermission(true, false, false)
        val NONE = UserPermission(false, false, false)
    }
}

class GroupPermission(read: Boolean = false, write: Boolean = false, execute: Boolean = false) : Permission(read, write, execute) {
    companion object {
        val RWX = GroupPermission(true, true, true)
        val RW_ = GroupPermission(true, true, false)
        val R__ = GroupPermission(true, false, false)
        val NONE = GroupPermission(false, false, false)
    }
}

class OtherPermission(read: Boolean = false, write: Boolean = false, execute: Boolean = false) : Permission(read, write, execute) {
    companion object {
        val RWX = OtherPermission(true, true, true)
        val RW_ = OtherPermission(true, true, false)
        val R__ = OtherPermission(true, false, false)
        val NONE = OtherPermission(false, false, false)
    }
}

data class FilePermissions(val user: UserPermission = UserPermission(), val group: GroupPermission = GroupPermission(), val other: OtherPermission = OtherPermission()) {
    fun renderChmod(): String = "u=${user.renderChmod()},g=${group.renderChmod()},o=${other.renderChmod()}"

    companion object {
        val RW_______ =
            FilePermissions(
                UserPermission.RW_,
                GroupPermission.NONE,
                OtherPermission.NONE,
            )
        val RW_R__R__ =
            FilePermissions(
                UserPermission.RW_,
                GroupPermission.R__,
                OtherPermission.R__,
            )
    }
}
