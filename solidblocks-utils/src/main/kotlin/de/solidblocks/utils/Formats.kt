package de.solidblocks.utils

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

fun dim(text: String) = escape(text, FORMATS.DIM)

fun underline(text: String) = escape(text, FORMATS.UNDERLINE)

fun strikethrough(text: String) = escape(text, FORMATS.STRIKETHROUGH)

fun italic(text: String) = escape(text, FORMATS.ITALIC)

fun code(text: String) = escape(color(text, COLORS.CYAN), FORMATS.BOLD)

fun command(text: String) = text

fun color(text: String, color: COLORS) =
    "${escapeCode(color.color)}${text}${escapeCode(RESETS.COLOR.reset)}"
