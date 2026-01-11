package de.solidblocks.cloudinit.model

import de.solidblocks.cloudinit.CloudInit1
import java.io.StringWriter


data class CloudInitUserData(var environmentVariables: Map<String, String> = mutableMapOf()) {
    val commands = ArrayList<CloudInitScriptCommand>()

    companion object {
        val SCRIPT_PLACEHOLDER: String = "__CLOUD_INIT_SCRIPT__"
        val VARIABLES_PLACEHOLDER: String = "__CLOUD_INIT_VARIABLES__"
    }

    fun render(): String {
        val template = CloudInit1::class.java.getResource("/blcks-cloud-init-bootstrap.sh.template").readText()

        val sw = StringWriter()

        commands.forEach {
            it.toShell().forEach { shell ->
                sw.appendLine(shell)
            }
        }

        val variables =
            mapOf("SOLIDBLOCKS_CLOUD_INIT_URL" to "https://test-blcks-bootstrap.s3.eu-central-1.amazonaws.com/blcks-cloud-init-v0.0.0.zip")
        return template.replace(VARIABLES_PLACEHOLDER, variables.entries.map {
            "export ${it.key}='${it.value}'"
        }.joinToString("\n")).replace(SCRIPT_PLACEHOLDER, sw.toString())
    }

    fun addCommand(command: CloudInitScriptCommand) = commands.add(command)
}