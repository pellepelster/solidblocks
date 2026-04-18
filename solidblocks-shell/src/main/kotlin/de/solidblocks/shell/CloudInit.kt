package de.solidblocks.shell

import de.solidblocks.shell.ShellScript.Companion.LIB_SOURCES_PATH
import java.io.StringWriter

class CloudInit() {

    data class CloudInitFile(val path: String, val content: String, val permissions: String)

    var privateKeyRsa: String? = null

    var privateKeyEd25519: String? = null

    val runCommands = ArrayList<String>()

    private val files = mutableListOf<CloudInitFile>()

    fun addRunCommand(runCommand: String) = runCommands.add(runCommand)

    fun addFile(path: String, content: String, permission: String) {
        files.add(CloudInitFile(path, content, permission))
    }

    fun render(): String {
        val sw = StringWriter()
        sw.appendLine("#cloud-config")

        if (privateKeyEd25519 != null || privateKeyRsa != null) {
            sw.appendLine("ssh_keys:")

            if (privateKeyEd25519 != null) {
                sw.appendLine("  ed25519_private: |")
                privateKeyEd25519!!.lines().forEach {
                    sw.appendLine("    $it")
                }
            }

            if (privateKeyRsa != null) {
                sw.appendLine("  rsa_private: |")
                privateKeyRsa!!.lines().forEach {
                    sw.appendLine("    $it")
                }
            }
        }

        if (files.isNotEmpty()) {
            sw.appendLine("write_files:")
            files.forEach {
                sw.appendLine("    - path: ${it.path}")
                sw.appendLine("      permissions: ${it.permissions}")
                sw.appendLine("      content: |")
                it.content.lines().forEach {
                    sw.appendLine("        $it")
                }
            }
        }

        sw.appendLine("runcmd:")
        runCommands.forEach {
            sw.appendLine("  - $it")
        }

        return sw.toString()
    }
}

fun ShellScript.toCloudInit(privateKeyRsa: String, privateKeyEd25519: String): CloudInit {
    val cloudInit = CloudInit()

    cloudInit.privateKeyRsa = privateKeyRsa
    cloudInit.privateKeyEd25519 = privateKeyEd25519

    this.libSources.forEach {
        cloudInit.addFile(it.key, it.value, "0444")
    }

    cloudInit.addFile("${LIB_SOURCES_PATH}/blcks-init.sh", this.render(false), "0544")
    cloudInit.addRunCommand("${LIB_SOURCES_PATH}/blcks-init.sh")

    return cloudInit
}
