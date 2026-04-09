package de.solidblocks.cloud.documentation

import de.solidblocks.cloud.configuration.*
import de.solidblocks.cloud.markdown
import kotlin.text.appendLine

class DocumentationGenerator(val hugo: Boolean = false) {

    fun generateMarkdown(factory: ConfigurationFactory<*>) = generateMarkdown(1, factory)

    fun SimpleKeyword<*>.example() = if (optional) {
        "$name: [$type]"
    } else {
        "$name: <$type>"
    }

    private fun ListKeyword<*>.generateListExample(example: StringBuilder, level: Int) {
        example.appendLine("${"  ".repeat(level)}${this.name}:")
        this.factory.keywords.filterIsInstance<SimpleKeyword<*>>().forEachIndexed { index, it ->
            if (index == 0) {
                example.appendLine("${"  ".repeat(level + 1)}- ${it.example()}")
            } else {
                example.appendLine("${"  ".repeat(level + 1)}  ${it.example()}")
            }
        }

        this.factory.keywords.filterIsInstance<ListKeyword<*>>().forEach {
            it.generateListExample(example, level + 2)
        }

        example.appendLine("${"  ".repeat(level + 2)}#...")
    }

    private fun ConfigurationFactory<*>.generateKeywordDocumentation(level: Int): String {
        val simpleTypes = this.keywords.filterIsInstance<SimpleKeyword<*>>()
        val listTypes = this.keywords.filterIsInstance<ListKeyword<*>>()

        return markdown {
            simpleTypes.forEach {
                h(level + 1, it.name)
                line {
                    italic("type")
                    text(": ")
                    bold(it.type.name)
                    text(", ")

                    italic("optional")
                    text(": ")
                    bold(it.optional.toString())
                    text(", ")
                }
                line {
                    when (it) {
                        is BaseStringKeyword<*> -> {
                            if (it.constraints.minLength != null) {
                                italic("min. length")
                                text(": ")
                                bold(it.constraints.minLength.toString())
                                text(", ")
                            }

                            if (it.constraints.maxLength != null) {
                                italic("max. length")
                                text(": ")
                                bold(it.constraints.maxLength.toString())
                                text(", ")
                            }

                            if (it.constraints.options.isNotEmpty()) {
                                italic("options")
                                text(": ")
                                bold(it.constraints.options.joinToString(", "))
                                text(", ")
                            }
                        }

                        else -> {}
                    }

                    italic("default")
                    text(": ")

                    if (hugo) {
                        bold("${it.default ?: "\\<none\\>"}")
                    } else {
                        bold("${it.default ?: "<none>"}")
                    }
                }

                p(it.help.description)
            }
            listTypes.forEach {
                h(level + 1, it.name)
                p(it.help.description)
                +it.factory.generateKeywordDocumentation(level + 1)
            }
        }
    }

    private fun generateMarkdown(level: Int, factory: ConfigurationFactory<*>, factoryKey: String? = null): String = markdown {
        val simpleTypes = factory.keywords.filterIsInstance<SimpleKeyword<*>>()
        val listTypes = factory.keywords.filterIsInstance<ListKeyword<*>>()
        val polymorphicListTypes = factory.keywords.filterIsInstance<PolymorphicListKeyword<*>>()

        if (level == 1 && !hugo || level > 1) {
            h(level, factory.help.title)
        }

        p(factory.help.description)

        val example = StringBuilder()

        if (factory is PolymorphicConfigurationFactory) {
            text("type: ${factoryKey ?: "unknown"}")
        }

        simpleTypes.forEach { example.appendLine(it.example()) }
        listTypes.forEach { it.generateListExample(example, 0) }

        polymorphicListTypes.forEach {
            example.append("\n")
            example.appendLine("${it.name}:")
            example.appendLine("  - type: <${it.factories.keys.joinToString("|")}>")
            example.appendLine("    #...")
        }

        codeBlock(example.toString())

        h(level + 1, "Keywords")
        +factory.generateKeywordDocumentation(level)

        polymorphicListTypes.forEach {
            h(level, it.name.capitalize())
            p(it.help.description)

            it.factories.entries.forEach { +generateMarkdown(level + 1, it.value, it.key) }
        }
    }
}
