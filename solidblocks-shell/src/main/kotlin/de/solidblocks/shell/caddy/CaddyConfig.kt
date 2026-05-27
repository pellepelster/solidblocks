package de.solidblocks.shell.caddy

data class ReverseProxy(val target: String)

enum class LogFormat {
    json,
    console,
    logfmt,
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

data class FileRoll(val size: String? = null, val keep: Int? = null, val keepFor: String? = null) {
    fun render(): List<String> = listOfNotNull(
        size?.let { "roll_size $it" },
        keep?.let { "roll_keep $it" },
        keepFor?.let { "roll_keep_for $it" },
    )
}

sealed interface LogOutput {
    fun render(): List<String>

    data object Stdout : LogOutput {
        override fun render(): List<String> = listOf("output stdout")
    }

    data object Stderr : LogOutput {
        override fun render(): List<String> = listOf("output stderr")
    }

    data object Discard : LogOutput {
        override fun render(): List<String> = listOf("output discard")
    }

    data class File(val path: String, val roll: FileRoll? = null) : LogOutput {
        override fun render(): List<String> = if (roll == null) {
            listOf("output file $path")
        } else {
            buildList {
                add("output file $path {")
                addAll(roll.render().map { "  $it" })
                add("}")
            }
        }
    }
}

data class Log(val output: LogOutput? = null, val format: LogFormat? = null, val level: LogLevel? = null) {

     companion object {
         fun default(path: String) = Log(
             LogOutput.File(path, FileRoll("10MiB", 10, "720h")),
             LogFormat.json,
             LogLevel.INFO,
         )
     }


    fun render(): List<String> = buildList {
        add("log {")
        output?.let { addAll(it.render().map { line -> "  $line" }) }
        format?.let { add("  format ${it.name}") }
        level?.let { add("  level ${it.name}") }
        add("}")
    }
}

data class Site(val address: String, val reverseProxy: ReverseProxy? = null, val log: Log? = null) {
    fun render(): List<String> = buildList {
        add("$address {")
        reverseProxy?.let { add("  reverse_proxy ${it.target}") }
        log?.let { addAll(it.render().map { line -> "  $line" }) }
        add("}")
    }
}

data class FileSystemStorage(val root: String) {
    fun render(): List<String> = listOf("storage file_system {", "  root $root", "}")
}

enum class AutoHttps {
    off,
    disable_redirects,
    ignore_loaded_certsm,
    disable_certs,
}

data class GlobalOptions(val storage: FileSystemStorage? = null, val email: String? = null, val autoHttps: AutoHttps? = null) {
    fun render(): List<String> = buildList {
        add("{")
        storage?.let { addAll(it.render().map { line -> "  $line" }) }
        email?.let { add("  email $it") }
        autoHttps?.let { add("  auto_https ${it.name}") }
        add("}")
    }
}

data class CaddyConfig(val globalOptions: GlobalOptions, val sites: List<Site> = emptyList()) {
    fun render(): String {
        val lines = buildList {
            addAll(globalOptions.render())
            add("")
            sites.forEach { addAll(it.render()) }
        }
        return lines.joinToString("\n", postfix = "\n")
    }
}
