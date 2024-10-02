package de.solidblocks.cli.workflow.conditions

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cli.utils.Empty
import de.solidblocks.cli.utils.Error
import de.solidblocks.cli.utils.Help
import de.solidblocks.cli.utils.Keyword
import de.solidblocks.cli.utils.KeywordHelp
import de.solidblocks.cli.utils.KeywordType
import de.solidblocks.cli.utils.Success
import de.solidblocks.cli.utils.valueForKeyword
import de.solidblocks.cli.workflow.YamlNodeFactory
import okio.FileSystem
import okio.Path.Companion.toPath

class FileCondition(private val file: String) : Condition {

    private val fs = FileSystem.SYSTEM

    override fun check() = fs.exists(file.toPath()).let {
        if (it) {
            CheckResult("file '${file}' exists", it)
        } else {
            CheckResult("file '${file}' not found", it)
        }
    }
}

enum class FileType { file, directory }

class FileConditionFactory : YamlNodeFactory<Condition> {

    private val keywordHelp = KeywordHelp(
        "/some/file",
        "Checks if a file or folder named */some/file* exists. IF no absolute path is provided, a patch relative to the location of the workflow file is assumed"
    )

    private val name = Keyword("name", KeywordType.string, keywordHelp)

    override val keyword = Keyword("file", KeywordType.string, keywordHelp)

    private val type = Keyword(
        "type",
        KeywordType.enum,
        KeywordHelp(
            "${FileType.file}",
            "Use `${FileType.file}` to check if **${name.name}** is a file, or `${FileType.directory}` to make sure its a directory"
        )
    )

    override val keywords = listOf(name, type)

    override fun parse(yamlNode: YamlNode) = when (val file = yamlNode.valueForKeyword(keyword, name)) {
        is Empty -> Empty(file.message)
        is Error -> Error(file.error)
        is Success -> Success(FileCondition(file.data))
    }

    override val help = Help("Evaluates presence and status of files and folders")

}

