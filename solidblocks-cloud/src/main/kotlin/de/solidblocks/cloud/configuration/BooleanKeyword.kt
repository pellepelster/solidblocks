package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.YamlEmpty
import de.solidblocks.cloud.utils.getBoolean
import de.solidblocks.cloud.utils.getOptionalBoolean

class BooleanKeyword<T : Boolean?> internal constructor(
    override val name: String,
    override val help: KeywordHelp,
    override val optional: Boolean,
    override val default: T?,
) : SimpleKeyword<T> {

    fun parse(yaml: YamlNode): Result<T> {
        val bool =
            if (optional) {
                when (val result = yaml.getOptionalBoolean(name)) {
                    is Error<Boolean?> -> return Error<T>(result.error)
                    is Success<Boolean?> -> result.data ?: default
                }
            } else {
                when (val result = yaml.getBoolean(name)) {
                    is YamlEmpty<Boolean> -> return Error<T>(result.message)
                    is Error<Boolean> -> return Error<T>(result.error)
                    is Success<Boolean> -> result.data
                }
            }

        return Success<T>(bool as T)
    }

    override val type = KeywordType.boolean
}

fun BooleanKeyword(name: String, help: KeywordHelp): BooleanKeyword<Boolean> = BooleanKeyword(name, help, optional = false, default = null)

fun BooleanKeyword<Boolean>.optional(): BooleanKeyword<Boolean?> = BooleanKeyword(name, help, optional = true, default = null)

fun BooleanKeyword<Boolean>.default(value: Boolean): BooleanKeyword<Boolean> = BooleanKeyword(name, help, optional = true, default = value)
