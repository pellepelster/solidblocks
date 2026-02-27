package de.solidblocks.cloud.documentation

import de.solidblocks.cloud.*
import de.solidblocks.cloud.configuration.*
import kotlin.text.appendLine

class DocumentationGenerator(val hugo: Boolean = false) {

    fun generateMarkdown(factory: ConfigurationFactory<*>): String {
        val markdown = MarkdownBuilder(hugo)
        generateMarkdown(markdown, 1, factory)
        return markdown.build()
    }

    fun SimpleKeyword<*>.example() = if (optional) {
        "${name}: [${type}]"
    } else {
        "${name}: <${type}>"
    }

    private fun ListKeyword<*>.generateListExample(example: StringBuilder, level: Int) {
        example.appendLine("${"  ".repeat(level)}${this.name}:")
        this.factory.keywords.filterIsInstance<SimpleKeyword<*>>().forEachIndexed {index, it ->
            if (index == 0) {
                example.appendLine("${"  ".repeat(level+1)}- ${it.example()}")
            } else {
                example.appendLine("${"  ".repeat(level+1)}  ${it.example()}")
            }
        }

        this.factory.keywords.filterIsInstance<ListKeyword<*>>().forEach {
            it.generateListExample(example, level + 2)
        }

        example.appendLine("${"  ".repeat(level+2)}#...")
    }

    private fun ConfigurationFactory<*>.generateKeywordDocumentation(markdown: MarkdownBuilder, level: Int) {
        val simpleTypes = this.keywords.filterIsInstance<SimpleKeyword<*>>()
        val listTypes = this.keywords.filterIsInstance<ListKeyword<*>>()

        simpleTypes.forEach {
            markdown.append(Header(level + 1, it.name))
            "${it.name} (${if (it.optional) "optional" else "required"})"
            markdown.append(Italic("type"), Text(": "), Bold(it.type.name), Text(","))
            markdown.append(Italic("optional"), Text(": "), Bold(it.optional), Text(","))
            when(it) {
                is BaseStringKeyword<*> -> {

                    if (it.constraints.minLength != null) {
                        markdown.append(Italic("min. length"), Text(": "), Bold(it.constraints.minLength.toString()), Text(","))
                    }

                    if (it.constraints.maxLength != null) {
                        markdown.append(Italic("max. length"), Text(": "), Bold(it.constraints.maxLength.toString()), Text(","))
                    }

                    if (it.constraints.options.isNotEmpty()) {
                        markdown.append(Italic("options"), Text(": "), Bold(it.constraints.options.joinToString(", ")))
                    }
                }
                else -> {}
            }
            markdown.append(Italic("default"), Text(": "), Bold("${it.default ?: "<none>"}"))

            markdown.append(Paragraph(it.help.description))
        }
        listTypes.forEach {
            markdown.append(Header(level + 1, it.name))
            markdown.append(Paragraph(it.help.description))
            it.factory.generateKeywordDocumentation(markdown, level + 1)
        }
    }

    private fun generateMarkdown(
        markdown: MarkdownBuilder,
        level: Int,
        factory: ConfigurationFactory<*>, factoryKey:
        String? = null
    ) {
        val simpleTypes = factory.keywords.filterIsInstance<SimpleKeyword<*>>()
        val listTypes = factory.keywords.filterIsInstance<ListKeyword<*>>()
        val polymorphicListTypes = factory.keywords.filterIsInstance<PolymorphicListKeyword<*>>()

        if (level == 1 && !hugo || level > 1) {
            markdown.append(Header(level, factory.help.title))
        }

        markdown.append(Paragraph(factory.help.description))

        val example = StringBuilder()

        if (factory is PolymorphicConfigurationFactory) {
            example.appendLine("type: ${factoryKey ?: "unknown"}")
        }

        simpleTypes.forEach {
            example.appendLine(it.example())
        }
        listTypes.forEach {
            it.generateListExample(example, 0)
        }

        polymorphicListTypes.forEach {
            example.append("\n")
            example.appendLine("${it.name}:")
            example.appendLine("  - type: <${it.factories.keys.joinToString("|")}>")
            example.appendLine("    #...")
        }

        markdown.append(Code(example.toString()))

        markdown.append(Header(level + 1, "Keywords"))
        factory.generateKeywordDocumentation(markdown, level)

        polymorphicListTypes.forEach {
            markdown.append(Header(level, it.name.capitalize()))
            markdown.append(Paragraph(it.help.description))

            it.factories.entries.forEach {
                generateMarkdown(markdown, level + 1, it.value, it.key)
            }
        }
    }
}
