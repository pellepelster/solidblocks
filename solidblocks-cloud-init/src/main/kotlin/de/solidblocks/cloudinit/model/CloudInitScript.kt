package de.solidblocks.cloudinit.model

import java.io.StringWriter
import kotlin.io.encoding.Base64

enum class Filesystem {
    ext4
}

data class Mount(val storageDevice: String, val storageDir: String, val filesystem: Filesystem = Filesystem.ext4)

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

data class File(val content: ByteArray, val path: String, val permissions: FilePermission = FilePermission())

data class CloudInitScript(var environmentVariables: Map<String, String> = mutableMapOf()) {
    val mounts = ArrayList<Mount>()
    val files = ArrayList<File>()

    fun render(): String {
        val sw = StringWriter()

        sw.appendLine("""
            #!/usr/bin/env bash
            set -eu -o pipefail
            DIR="$(cd "$(dirname "$0")" ; pwd -P)"
            
        """.trimIndent())

        for (file in files) {
            sw.appendLine("touch ${file.path}")
            sw.appendLine("chmod ${file.permissions.renderChmod()} ${file.path}")
            sw.appendLine("echo \"${Base64.encode(file.content)}\" | base64 -d > ${file.path}")
        }

        return sw.toString()
    }
}