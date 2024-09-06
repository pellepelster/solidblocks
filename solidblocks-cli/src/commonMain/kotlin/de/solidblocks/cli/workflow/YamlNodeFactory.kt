package de.solidblocks.cli.workflow

import com.charleskorn.kaml.YamlNode
import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.terminal.Terminal
import de.solidblocks.cli.utils.Help
import de.solidblocks.cli.utils.Keyword
import de.solidblocks.cli.utils.Result
import de.solidblocks.cli.utils.indentWithYamlObjectMarker

interface YamlNodeFactory<T> {
    val keyword: Keyword
    val keywords: List<Keyword>
    val help: Help

    fun parse(yamlNode: YamlNode): Result<out T>

    fun maxExampleLineLength() =
        (listOf(keyword) + keywords).map { "${it.name}: ${it.help.example}" }.maxOf { it.length }

    fun shortHandExample(terminal: Terminal) = listOf(keyword).map {
        "${it.name}: ${it.help.example}".padEnd(maxExampleLineLength()) + " # ${terminal.render(Markdown(it.help.description))}"
    }


    fun extendedExample(terminal: Terminal) =
        (listOf("${keyword.name}:") + keywords.map {
            "${it.name}: ${it.help.example}".padEnd(maxExampleLineLength()) + " # ${terminal.render(Markdown(it.help.description))}"
        })

    fun helpExample(terminal: Terminal) =
        (shortHandExample(terminal).indentWithYamlObjectMarker() + listOf("") + extendedExample(terminal).indentWithYamlObjectMarker() + listOf(
            ""
        )).joinToString("\n")

    fun help(): String {
        val keywordsExample = keywords.joinToString("\n") {
            "  ${it.name}: ${it.help.example}"
        }

        val keywordsHelp = keywords.joinToString("\n") {
            "**${it.name}**: ${it.help.description}"
        }

        return listOf(
            "## ${keyword.name.capitalize()}",
            help.description,
            "### Shorthand usage",
            "```",
            "- ${keyword.name}: ${keyword.help.example}",
            "```",
            "",
            keyword.help.description,
            "",
            "### Extended usage",
            "```",
            "- ${keyword.name}:",
            keywordsExample,
            "```",
            "",
            keywordsHelp
        ).joinToString("\n")
    }
}