package de.solidblocks.cloud

import java.io.StringWriter

fun markdown(block: MarkdownBuilder.() -> Unit): String = MarkdownBuilder().apply(block).build()

class MarkdownBuilder {

  private val parts = mutableListOf<String>()

  fun h(level: Int, text: String) {
    parts += "${"#".repeat(level)} $text"
  }

  fun h1(text: String) = h(1, text)

  fun h2(text: String) = h(2, text)

  fun h3(text: String) = h(3, text)

  fun h4(text: String) = h(4, text)

  fun h5(text: String) = h(5, text)

  fun h6(text: String) = h(6, text)

  fun p(block: InlineBuilder.() -> Unit) {
    parts += "\n${InlineBuilder().apply(block).build()}\n"
  }

  fun p(text: String) {
    parts += "\n${text}\n"
  }

  fun line(block: InlineBuilder.() -> Unit) {
    parts += InlineBuilder().apply(block).build()
  }

  fun text(text: String) {
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

  fun italic(text: String) {
    parts += "*$text*"
  }

  fun br() {
    parts += ""
  }

  operator fun String.unaryPlus() {
    parts += this
  }

  operator fun String.plus(other: String): String {
    parts += this
    return other
  }

  fun build(): String = parts.joinToString("\n")
}

class InlineBuilder {

  private val sb = StringBuilder()

  fun bold(text: String) {
    sb.append("**$text**")
  }

  fun italic(text: String) {
    sb.append("*$text*")
  }

  fun text(text: String) {
    sb.append(text)
  }

  operator fun String.unaryPlus() {
    sb.append(this)
  }

  operator fun String.plus(other: String): String {
    sb.append(this)
    return other
  }

  fun build(): String = sb.toString()
}

class ListBuilder {

  private val items = mutableListOf<String>()

  fun item(item: String) {
    items.add(item)
  }

  fun build(): String {
    val sw = StringWriter()
    items.forEach { sw.appendLine("* $it") }

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
    val widths =
        IntArray(colCount) { i ->
          maxOf(
              headers.getOrElse(i) { "" }.length,
              rows.maxOfOrNull { it.getOrElse(i) { "" }.length } ?: 0,
          )
        }

    fun List<String>.renderRow() =
        "| " + indices.joinToString(" | ") { i -> getOrElse(i) { "" }.padEnd(widths[i]) } + " |"

    val separator = "| " + widths.joinToString(" | ") { "-".repeat(it) } + " |"

    return buildString {
          appendLine(headers.renderRow())
          appendLine(separator)
          rows.forEach { appendLine(it.renderRow()) }
        }
        .trimEnd()
  }
}
