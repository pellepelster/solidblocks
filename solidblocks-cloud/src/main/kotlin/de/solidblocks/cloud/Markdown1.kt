import java.io.StringWriter

fun markdown(block: MarkdownBuilder.() -> Unit): String =
    MarkdownBuilder().apply(block).build()

class MarkdownBuilder {

    private val parts = mutableListOf<String>()

    fun h1(text: String) {
        parts += "# $text"
    }

    fun h2(text: String) {
        parts += "## $text"
    }

    fun h3(text: String) {
        parts += "### $text"
    }

    fun h4(text: String) {
        parts += "#### $text"
    }

    fun h5(text: String) {
        parts += "##### $text"
    }

    fun h6(text: String) {
        parts += "###### $text"
    }

    fun p(block: InlineBuilder.() -> Unit) {
        parts += InlineBuilder().apply(block).build()
    }

    fun p(text: String) {
        parts += text
    }

    fun codeBlock(language: String = "", block: () -> String) {
        parts += "```$language\n${block().trimIndent()}\n```"
    }

    fun codeBlock(language: String = "", code: String) {
        parts += "```$language\n${code.trimIndent()}\n```"
    }

    fun codeBlock(code: String) {
        parts += "```\n${code.trimIndent()}\n```"
    }

    fun table(block: TableBuilder.() -> Unit) {
        parts += TableBuilder().apply(block).build()
    }

    fun list(block: ListBuilder.() -> Unit) {
        parts += ListBuilder().apply(block).build()
    }

    fun hr() {
        parts += "---"
    }

    fun bold(text: String) {
        parts += "**$text**"
    }

    fun br() {
        parts += ""
    }

    fun build(): String = parts.joinToString("\n\n")
}

/*
fun bold(text: String): String = "**$text**"

fun italic(text: String): String = "*$text*"

fun boldItalic(text: String): String = "***$text***"

fun code(text: String): String = "`$text`"
*/

class InlineBuilder {

    private val sb = StringBuilder()

    operator fun String.unaryPlus() {
        sb.append(this)
    }

    operator fun String.plus(other: String): String {
        sb.append(this)
        return other
    }

    fun build(): String {
        return sb.toString()
    }
}

class ListBuilder {

    private val items = mutableListOf<String>()

    fun item(item: String) {
        items.add(item)
    }

    fun build(): String {
        val sw = StringWriter()
        items.forEach {
            sw.appendLine("* ${it}")
        }

        return sw.toString()
    }

}

class TableBuilder {

    private var headers: List<String> = emptyList()
    private val rows = mutableListOf<List<String>>()

    fun header(vararg columns: String) {
        headers = columns.toList()
    }

    fun row(vararg cells: String) {
        rows += cells.toList()
    }

    fun build(): String {
        require(headers.isNotEmpty()) { "A table must have at least one header column." }

        val colCount = headers.size
        val widths = IntArray(colCount) { i ->
            maxOf(
                headers.getOrElse(i) { "" }.length,
                rows.maxOfOrNull { it.getOrElse(i) { "" }.length } ?: 0
            )
        }

        fun List<String>.renderRow() =
            "| " + indices.joinToString(" | ") { i ->
                getOrElse(i) { "" }.padEnd(widths[i])
            } + " |"

        val separator = "| " + widths.joinToString(" | ") { "-".repeat(it) } + " |"

        return buildString {
            appendLine(headers.renderRow())
            appendLine(separator)
            rows.forEach { appendLine(it.renderRow()) }
        }.trimEnd()
    }
}
