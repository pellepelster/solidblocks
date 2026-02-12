package de.solidblocks.caddy

import java.io.StringWriter

data class ReverseProxy(val target: String)

data class Site(val address: String, val reverseProxy: ReverseProxy? = null)

data class FileSystemStorage(val root: String)

@Suppress("ktlint:standard:enum-entry-name-case")
enum class AutoHttps {
  off,
  disable_redirects,
  ignore_loaded_certsm,
  disable_certs,
}

data class GlobalOptions(
    val storage: FileSystemStorage? = null,
    val email: String? = null,
    val autoHttps: AutoHttps? = null,
)

data class CaddyConfig(val globalOptions: GlobalOptions, val sites: List<Site> = emptyList()) {

  fun render(): String {
    val sw = StringWriter()

    sw.appendLine("{")
    globalOptions.storage?.let {
      sw.appendLine("  storage file_system {")
      sw.appendLine("    root ${it.root}")
      sw.appendLine("  }")
    }
    globalOptions.email?.let { sw.appendLine("  email $it") }
    globalOptions.autoHttps?.let { sw.appendLine("  auto_https ${it.name}") }
    sw.appendLine("}")
    sw.appendLine()
    sites.forEach {
      sw.appendLine("${it.address} {")
      if (it.reverseProxy != null) {
        sw.appendLine("  reverse_proxy ${it.reverseProxy.target}")
      }
      sw.appendLine("}")
    }
    return sw.toString()
  }
}
