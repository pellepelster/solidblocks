package de.solidblocks.cloud

import kotlin.math.min

sealed class Markdown {
    abstract fun render(hugo: Boolean): String
}

open class Header(val level: Int, val title: String) : Markdown() {
    override fun render(hugo: Boolean) = "${"#".repeat(min(6, level))} $title"
}

class Header1(title: String) : Header(1, title)
class Header2(title: String) : Header(2, title)
class Header3(title: String) : Header(3, title)
class Header4(title: String) : Header(4, title)
class Header5(title: String) : Header(5, title)
class Header6(title: String) : Header(6, title)

fun escape(text: Any, hugo: Boolean) = if (hugo) {
    text.toString().replace("<", "\\<").replace(">", "\\>").replace("~", "\\~")
} else {
    text.toString()
}

class Text(val text: Any) : Markdown() {
    override fun render(hugo: Boolean) = escape(text, hugo)
}

class Bold(val text: Any) : Markdown() {
    override fun render(hugo: Boolean) = "**${escape(text, hugo)}**"
}

class Italic(val text: Any) : Markdown() {
    override fun render(hugo: Boolean) = "*${escape(text, hugo)}*"
}

class Code(val content: String) : Markdown() {
    override fun render(hugo: Boolean) =
        """
```yaml
$content
```
    """.trimIndent()
}

class Paragraph(val content: String) : Markdown() {
    override fun render(hugo: Boolean) = "\n${escape(content, hugo)}\n"
}

class MarkdownBuilder(val hugo: Boolean) {

    val markdown = mutableListOf<List<Markdown>>()

    fun append(vararg m: Markdown) {
        markdown.add(m.toList())
    }

    fun appendNewline() = append(Text("\n"))

    fun build() = markdown.joinToString("\n") { it.joinToString("") { it.render(hugo) } }
}
