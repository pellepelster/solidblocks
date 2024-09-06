package de.solidblocks.cli.workflow.tasks

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cli.utils.*

class Command : TaskFactory {
    override val keyword = Keyword(
        "command",
        KeywordType.string,
        KeywordHelp("/some/command", "Runs the command */some/command* and waits until it finishes with exit code 0")
    )

    private val name = Keyword("name", KeywordType.string, keyword.help)

    override val keywords = listOf(name)

    override val help = Help("Run commands and scripts")

    override fun parse(yamlNode: YamlNode): Result<Any> {
        TODO("Not yet implemented")
    }
}