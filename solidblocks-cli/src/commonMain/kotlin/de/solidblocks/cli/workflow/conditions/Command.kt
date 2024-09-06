import com.charleskorn.kaml.YamlNode
import de.solidblocks.cli.commandExists
import de.solidblocks.cli.utils.Empty
import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Help
import de.solidblocks.cli.utils.Keyword
import de.solidblocks.cli.utils.KeywordHelp
import de.solidblocks.cli.utils.KeywordType
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.utils.valueForKeyword
import de.solidblocks.cli.workflow.YamlNodeFactory
import de.solidblocks.cli.workflow.conditions.CheckResult
import de.solidblocks.cli.workflow.conditions.Condition


class CommandCondition(val command: String) : Condition {

    override fun check() = if (commandExists(command)) {
        CheckResult("command '${command}' exists", true)
    } else {
        CheckResult("command '${command}' not found", false)
    }
}

class CommandConditionFactory : YamlNodeFactory<Condition> {

    override val keyword =
        Keyword("command", KeywordType.string, KeywordHelp("ping", "Checks if the *ping* is is available on the path"))

    private val name = Keyword("name", KeywordType.string, keyword.help)

    override val keywords = listOf(name)

    override fun parse(yamlNode: YamlNode) = when (val file = yamlNode.valueForKeyword(keyword, name)) {
        is Empty -> Empty(file.message)
        is Error -> Error(file.error)
        is Success -> Success(CommandCondition(file.data))
    }

    override val help = Help("Evaluates availability of commands")
}
