package de.solidblocks.cloud

sealed class Markdown {
  abstract fun render(): String
}

class Header1(val title: String) : Markdown() {
  override fun render() = "# $title"
}

class Header2(val title: String) : Markdown() {
  override fun render() = "## $title"
}

class Code(val content: String) : Markdown() {
  override fun render() =
      """
```yaml
$content
```
    """
          .trimIndent()
}

class Paragraph(val content: String) : Markdown() {
  override fun render() = "\n${content}\n"
}

class MarkdownBuilder {

  val markdown = mutableListOf<Markdown>()

  fun append(m: Markdown) {
    markdown.add(m)
  }

  fun build() = markdown.joinToString("\n") { it.render() }
}
