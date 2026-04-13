package de.solidblocks.cloud.utils

import java.io.InputStreamReader
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun getEnvOrProperty(name: String) = System.getenv(name) ?: System.getProperty(name)

fun commandExists(command: String) = try {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val checkCommand =
        if (isWindows) {
            listOf("cmd.exe", "/c", "where", command)
        } else {
            listOf("sh", "-c", "command -v $command")
        }

    val process =
        ProcessBuilder(checkCommand)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    process.waitFor() == 0
} catch (e: Exception) {
    false
}

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

fun runCommand(command: List<String>, stdin: String? = null) = try {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")

    val process = ProcessBuilder(command).start()

    if (stdin != null) {
        process.outputStream.use { output ->
            output.write(stdin.toByteArray())
            output.flush()
            output.close()
        }
    }

    val exitCode = process.waitFor()

    CommandResult(
        exitCode,
        InputStreamReader(process.inputStream).readText(),
        InputStreamReader(process.errorStream).readText(),
    )
} catch (e: Exception) {
    null
}

class ByteSize(val bytes: Long) {

    fun gigabytes() = (bytes / 1000 / 1000 / 1000).toInt()

    companion object {
        fun fromGigabytes(gigabytes: Int) = ByteSize(gigabytes.toLong() * 1000 * 1000 * 1000)
    }
}

fun Instant.formatLocale(zone: ZoneId = ZoneId.systemDefault()) = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zone).format(this)

fun Duration.formatLocale() = if (this.toMinutes() > 2) {
    "${this.toMinutes()}m"
} else {
    "${this.toSeconds()}s"
}

fun Long.formatBytes(): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = this.toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex++
    }

    return if (unitIndex == 0) {
        "$this ${units[unitIndex]}"
    } else {
        "${"%.2f".format(value)} ${units[unitIndex]}"
    }
}

public infix fun <T> List<T>.equalsIgnoreOrder(other: List<T>) = this.size == other.size && this.toSet() == other.toSet()

public fun <T> Iterable<T>.joinToStringOrEmpty(separator: CharSequence = ", ", empty: String = "<none>", transform: (T) -> CharSequence): String = if (this.count() == 0) {
    empty
} else {
    this.joinToString(separator) { transform.invoke(it) }
}
