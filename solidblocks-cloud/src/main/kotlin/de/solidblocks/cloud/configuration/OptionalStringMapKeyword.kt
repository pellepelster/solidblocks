package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.YamlEmpty
import de.solidblocks.cloud.utils.YamlError
import de.solidblocks.cloud.utils.YamlSuccess
import de.solidblocks.cloud.utils.getStringMap

class OptionalStringMapKeyword(override val name: String, override val help: KeywordHelp) : SimpleKeyword<Map<String, String>?> {

    fun parse(yaml: YamlNode): Result<Map<String, String>?> = when (val result = yaml.getStringMap(name)) {
        is YamlError<Map<String, String>> -> Error<Map<String, String>?>(result.error)
        is YamlSuccess<Map<String, String>> -> Success<Map<String, String>?>(result.data)
        is YamlEmpty<Map<String, String>> -> Success(null)
    }

    override val optional = true

    override val default: Map<String, String>? = null

    override val type = KeywordType.map
}
