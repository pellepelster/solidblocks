package de.solidblocks.cloudinit.model

import de.solidblocks.shell.LibraryCommand
import kotlin.io.encoding.Base64

class File(val content: ByteArray, val path: String, val permissions: FilePermissions = FilePermissions()) :
    LibraryCommand {

    override fun toShell() = listOf(
        "touch ${path}",
        "chmod ${permissions.renderChmod()} ${path}",
        "echo \"${Base64.encode(content)}\" | base64 -d > ${path}"
    )
}

sealed class Permission(val read: Boolean, val write: Boolean, val execute: Boolean) {
    fun renderChmod() = "${if (read) "r" else "-"}${if (write) "w" else "-"}${if (execute) "x" else "-"}"
}

class UserPermission(read: Boolean = true, write: Boolean = true, execute: Boolean = false) :
    Permission(read, write, execute) {
    companion object {
        val RWX = UserPermission(true, true, true)
        val R__ = UserPermission(true, false, false)
    }
}

class GroupPermission(read: Boolean = false, write: Boolean = false, execute: Boolean = false) :
    Permission(read, write, execute) {
    companion object {
        val RWX = GroupPermission(true, true, true)
        val R__ = GroupPermission(true, false, false)
    }
}

class OtherPermission(read: Boolean = false, write: Boolean = false, execute: Boolean = false) :
    Permission(read, write, execute) {
    companion object {
        val RWX = OtherPermission(true, true, true)
        val R__ = OtherPermission(true, false, false)
    }
}

data class FilePermissions(
    val user: UserPermission = UserPermission(),
    val group: GroupPermission = GroupPermission(),
    val other: OtherPermission = OtherPermission()
) {
    fun renderChmod(): String {
        return "u=${user.renderChmod()},g=${group.renderChmod()},o=${other.renderChmod()}"
    }
}