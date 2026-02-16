package de.solidblocks.cloud.documentation

import de.solidblocks.cloud.*
import de.solidblocks.cloud.configuration.ComplexKeyword
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.SimpleKeyword

class DocumentationGenerator(val factory: ConfigurationFactory<*>) {

  fun generateMarkdown(): String {
    val markdown = MarkdownBuilder()

    val simpleTypes = factory.keywords.filterIsInstance<SimpleKeyword<*>>()
    val complexTypes = factory.keywords.filterIsInstance<ComplexKeyword<*>>()

    markdown.append(Header1(factory.help.title))
    markdown.append(Paragraph(factory.help.description))

    val example = StringBuilder()
    simpleTypes.forEach { example.appendLine("- ${it.name}: <${it.type}>") }
    complexTypes.forEach { example.appendLine("- ${it.name}: <${it.type}>") }
    markdown.append(Code(example.toString()))

    simpleTypes.forEach {
      markdown.append(Header2(it.name))
      markdown.append(Paragraph(it.help.description))
    }

    complexTypes.forEach {
      markdown.append(Header1(it.factory.help.title))
      markdown.append(Paragraph(it.factory.help.description))
    }

    return markdown.build()
  }
}
