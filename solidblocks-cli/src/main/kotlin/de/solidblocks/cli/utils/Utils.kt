package de.solidblocks.cli.utils

import de.solidblocks.cli.hetzner.api.HetznerNamedResource
import kotlin.reflect.KClass

public enum class LogType {
    BLCKS,
    STDOUT,
    STDERR,
}

fun logInfo(message: String, logType: LogType = LogType.BLCKS) = println("[${logType.name.lowercase()}] $message")

fun logError(message: String, logType: LogType = LogType.BLCKS) =
    println("[${logType.name.lowercase()}] ${color(message, COLORS.RED)}")

fun logSuccess(message: String, logType: LogType = LogType.BLCKS) =
    println("[${logType.name.lowercase()}] ${color(message, COLORS.GREEN)}")

fun logWarning(message: String, logType: LogType = LogType.BLCKS) =
    println("[${logType.name.lowercase()}] ${color(message, COLORS.YELLOW)}")

fun logDebug(message: String, logType: LogType = LogType.BLCKS) =
    println("[${logType.name.lowercase()}] ${color(message, COLORS.BRIGHT_BLUE)}")

sealed interface Result<T>

data class Success<T>(val data: T) : Result<T>

data class Error<T>(val error: String) : Result<T>

class Empty<T>(val message: String) : Result<T>

fun Collection<Result<*>>.hasError() = this.any { it is Error<*> }

fun Collection<Result<*>>.aggregateErrors() =
    this.filterIsInstance<Error<*>>().joinToString { it.error }

fun <T> Collection<Result<*>>.mapSuccess() = this.filterIsInstance<Success<T>>().map { it.data }

public fun List<String>.indentWithYamlObjectMarker() =
    this.withIndex().map {
        if (it.index == 0) {
            "- ${it.value}"
        } else {
            "  ${it.value}"
        }
    }

enum class FORMATS(val start: Int, val reset: Int) {
    BOLD(1, 22),
    DIM(2, 22),
    ITALIC(3, 23),
    UNDERLINE(4, 24),
    STRIKETHROUGH(9, 29),
}

enum class COLORS(val color: Int, val background: Int) {
    RED(31, 41),
    GREEN(32, 42),
    YELLOW(33, 43),
    MAGENTA(35, 45),
    CYAN(36, 46),
    BRIGHT_BLUE(94, 104),
}

enum class RESETS(val reset: Int) {
    ALL(0),
    COLOR(39),
    BACKGROUND(49),
}

fun escapeCode(code: Int) = "\u001b[${code}m"

fun escapeCode(format: FORMATS) = escapeCode(format.start)

fun resetCode(format: FORMATS) = escapeCode(format.reset)

fun escape(text: String, vararg formats: FORMATS) =
    "${formats.joinToString("") { escapeCode(it) }}${text}${formats.joinToString("") { resetCode(it) }}"

fun bold(text: String) = escape(text, FORMATS.BOLD)

fun code(text: String) = escape(color(text, COLORS.CYAN), FORMATS.BOLD)

fun command(text: String) = text

fun color(text: String, color: COLORS) =
    "${escapeCode(color.color)}${text}${escapeCode(RESETS.COLOR.reset)}"

fun String.pascalCaseToWhiteSpace() = this.replace(Regex("([A-Z])"), " $1").trim()

fun KClass<out HetznerNamedResource>.pascalCaseToWhiteSpace() =
    this.simpleName!!.removeSuffix("Response").pascalCaseToWhiteSpace().lowercase()
