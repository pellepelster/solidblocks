package de.solidblocks.cloud.documentation

import de.solidblocks.cloud.*
import de.solidblocks.cloud.configuration.*

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
            example.appendLine("${it.name}:")
            it.factory.keywords.filterIsInstance<SimpleKeyword<*>>().forEach {
                example.appendLine("  ${it.example()}")
            }
        }

        polymorphicListTypes.forEach {
            example.append("\n")
            example.appendLine("${it.name}:")
            example.appendLine("  - type: <${it.factories.keys.joinToString("|")}>")
            example.appendLine("    #...")
        }

        markdown.append(Code(example.toString()))

        markdown.append(Header(level + 1, "Keywords"))

        simpleTypes.forEach {
            markdown.append(Header(level + 1, it.name))
            "${it.name} (${if (it.optional) "optional" else "required"})"
            markdown.append(Italic("type"), Text(": "), Bold(it.type.name), Text("\n"))
            markdown.append(Italic("optional"), Text(": "), Bold(it.optional), Text("\n"))
            markdown.append(Italic("default"), Text(": "), Bold("${it.default ?: "<none>"}"), Text("\n"))

            markdown.append(Paragraph(it.help.description))
        }

        polymorphicListTypes.forEach {
            markdown.append(Header(level, it.name))

            it.factories.entries.forEach {
                generateMarkdown(markdown, level + 1, it.value, it.key)
            }
        }
    }
}
