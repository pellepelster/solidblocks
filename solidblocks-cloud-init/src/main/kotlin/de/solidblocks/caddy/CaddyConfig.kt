package de.solidblocks.caddy

import java.io.StringWriter

data class Site(val address: String)

data class FileSystemStorage(val root: String)

data class GlobalOptions(val storage: FileSystemStorage? = null, val email: String? = null)

data class CaddyConfig(val globalOptions: GlobalOptions, val sites: List<Site> = emptyList()) {

    fun render(): String {
        val sw = StringWriter()

        sw.appendLine("{")
        globalOptions.storage?.let {
            sw.appendLine("  storage file_system {")
            sw.appendLine("    root ${it.root}")
            sw.appendLine("  }")
        }
        globalOptions.email?.let {
            sw.appendLine("  email ${it}")
        }
        sw.appendLine("}")
        sw.appendLine()
        sites.forEach {
            sw.appendLine("${it.address} {")
            sw.appendLine("}")
        }
        return sw.toString()
    }

}