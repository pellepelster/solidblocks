package de.solidblocks.cloudinit.model

import kotlin.io.encoding.Base64

class File(val content: ByteArray, val path: String, val permissions: FilePermission = FilePermission()) :
    CloudInitScriptCommand {
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
    Permission(read, write, execute)

class GroupPermission(read: Boolean = false, write: Boolean = false, execute: Boolean = false) :
    Permission(read, write, execute)

class OtherPermission(read: Boolean = false, write: Boolean = false, execute: Boolean = false) :
    Permission(read, write, execute)

data class FilePermission(
    val user: UserPermission = UserPermission(),
    val group: GroupPermission = GroupPermission(),
    val other: OtherPermission = OtherPermission()
) {
    fun renderChmod(): String {
        return "u=${user.renderChmod()},g=${group.renderChmod()},o=${other.renderChmod()}"
    }
}