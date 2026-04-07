package de.solidblocks.cloud

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MarkdownTest {

  @Test
  fun testBold() {
    markdown { bold("bold") } shouldBe "**bold**"
  }

  @Test
  fun testItalic() {
    markdown { italic("italic") } shouldBe "*italic*"
  }

  @Test
  fun testCode() {
    markdown { codeBlock("some code") } shouldBe "```\nsome code\n```"
  }

  @Test
  fun testLine() {
    markdown {
      line {
        bold("italic")
        italic("italic")
      }
    } shouldBe "**italic***italic*"
  }

  @Test
  fun testParagraphBuilder() {
    markdown { p { text("some text") } } shouldBe "\nsome text\n"
  }

  @Test
  fun testParagraph() {
    markdown { p("some text") } shouldBe "\nsome text\n"
  }

  @Test
  fun testNestedMarkdown() {
    markdown {
      +markdown { bold("bold") }
      +markdown { italic("italic") }
    } shouldBe "**bold**\n*italic*"
  }

  @Test
  fun testMultipleElements() {
    markdown {
      bold("bold")
      italic("italic")
    } shouldBe "**bold**\n*italic*"
  }

  @Test
  fun sandbox() {
    val doc = markdown {
      h1("Header 1")
      bold("bold")
      // p("Plain text, ${bold("bold text")}, ${italic("italic text")}, and
      // ${boldItalic("bold-italic")}.")
      // p("Inline code: ${code("val x = listOf(1, 2, 3)")}.")

      h2("Code Block")
      codeBlock("kotlin") {
        """
        fun greet(name: String): String {
            return "Hello, ${'$'}name!"
        }
        """
            .trimIndent()
      }

      h2("Table")
      list { item("yolo") }
      table {
        header("Language", "Paradigm", "Runs on JVM")
        row("Kotlin", "Multi-paradigm", "Yes")
        row("Python", "Multi-paradigm", "No")
        row("Scala", "Functional", "Yes")
      }

      hr()
      p("Generated with the Markdown DSL.")
    }

    println(doc)
  }
}
