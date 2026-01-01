package de.solidblocks.cloudinit.model

import de.solidblocks.cloudinit.CloudInit1
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

    companion object {
        val SCRIPT_PLACEHOLDER: String = "__CLOUD_INIT_SCRIPT__"

        val VARIABLES_PLACEHOLDER: String = "__CLOUD_INIT_VARIABLES__"
    }

    fun render(): String {
        val template = CloudInit1::class.java.getResource("/blcks-cloud-init-bootstrap.sh.template").readText()

        val sw = StringWriter()

        for (file in files) {
            sw.appendLine("touch ${file.path}")
            sw.appendLine("chmod ${file.permissions.renderChmod()} ${file.path}")
            sw.appendLine("echo \"${Base64.encode(file.content)}\" | base64 -d > ${file.path}")
        }

        sw.appendLine()
        mounts.forEach {
            sw.appendLine("storage_mount ${it.storageDevice} ${it.storageDir} ${it.filesystem.name}")
        }

        val variables = mapOf("SOLIDBLOCKS_CLOUD_INIT_URL" to "https://test-blcks-bootstrap.s3.eu-central-1.amazonaws.com/solidblocks/solidblocks-cloud-init/v0.0.0/solidblocks-cloud-init-v0.0.0.zip")
        return template.replace(VARIABLES_PLACEHOLDER, variables.entries.map {
            "export ${it.key}='${it.value}'"
        }.joinToString("\n")).replace(SCRIPT_PLACEHOLDER, sw.toString())
    }
}