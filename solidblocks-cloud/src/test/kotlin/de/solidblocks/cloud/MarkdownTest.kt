package de.solidblocks.cloud

import de.solidblocks.cloud.utils.markdown
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MarkdownTest {

    @Test
    fun `bold`() {
        markdown { bold("bold") } shouldBe "**bold**"
    }

    @Test
    fun `italic`() {
        markdown { italic("italic") } shouldBe "*italic*"
    }

    @Test
    fun `code`() {
        markdown { codeBlock("some code") } shouldBe "```\nsome code\n```"
    }

    @Test
    fun `line`() {
        markdown {
            line {
                bold("italic")
                italic("italic")
            }
        } shouldBe "**italic***italic*"
    }

    @Test
    fun `paragraph builder`() {
        markdown { p { text("some text") } } shouldBe "\nsome text\n"
    }

    @Test
    fun `paragraph`() {
        markdown { p("some text") } shouldBe "\nsome text\n"
    }

    @Test
    fun `nested markdown`() {
        markdown {
            +markdown { bold("bold") }
            +markdown { italic("italic") }
        } shouldBe "**bold**\n*italic*"
    }

    @Test
    fun `multiple elements`() {
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
