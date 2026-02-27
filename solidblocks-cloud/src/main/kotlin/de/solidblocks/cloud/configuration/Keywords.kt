package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.YamlEmpty
import de.solidblocks.cloud.utils.getList
import de.solidblocks.cloud.utils.getNonNullOrEmptyString
import de.solidblocks.cloud.utils.getObject
import de.solidblocks.cloud.utils.getOptionalBoolean
import de.solidblocks.cloud.utils.getOptionalString
import de.solidblocks.cloud.utils.getPolymorphicList

enum class KeywordType {
    boolean,
    string,
    list,
    polymorphic_list,
    `object`,
}

sealed interface Keyword<T> {
    val name: String
    val type: KeywordType
}

sealed interface SimpleKeyword<T> : Keyword<T> {
    val help: KeywordHelp
    val optional: Boolean
    val default: T?
}

sealed interface ComplexKeyword<T> : Keyword<T> {
    val factory: ConfigurationFactory<T>
}

sealed interface ComplexPolymorphicKeyword<T> : Keyword<T> {
    val factories: Map<String, ConfigurationFactory<out T>>
}

data class ListKeyword<T>(
    override val name: String,
    override val factory: ConfigurationFactory<T>,
    val help: KeywordHelp,
) : ComplexKeyword<T> {

    fun parse(yaml: YamlNode): Result<List<T>> {
        val list =
            when (val list = yaml.getList(name, factory)) {
                is YamlEmpty<List<T>> -> emptyList()
                is Error<List<T>> -> return Error(list.error)
                is Success<List<T>> -> list.data
            }

        return Success(list)
    }

    override val type = KeywordType.list
}

data class PolymorphicListKeyword<T>(
    override val name: String,
    override val factories: Map<String, PolymorphicConfigurationFactory<out T>>,
    val help: KeywordHelp,
) : ComplexPolymorphicKeyword<T> {

    fun parse(yaml: YamlNode): Result<List<T>> {
        val list =
            when (val list = yaml.getPolymorphicList(name, factories)) {
                is YamlEmpty<List<T>> -> emptyList()
                is Error<List<T>> -> return Error(list.error)
                is Success<List<T>> -> list.data
            }

        return Success(list)
    }

    override val type = KeywordType.polymorphic_list
}

data class ObjectKeyword<T>(
    override val name: String,
    override val factory: ConfigurationFactory<T>,
    val help: KeywordHelp,
) : ComplexKeyword<T> {

    fun parse(yaml: YamlNode): Result<T> {
        val obj =
            when (val list = yaml.getObject(name, factory)) {
                is Error<T> -> return Error(list.error)
                is Success<T> -> list.data
            }

        return Success(obj)
    }

    override val type = KeywordType.`object`
}

data class StringKeyword(override val name: String, override val help: KeywordHelp) :
    SimpleKeyword<String> {

    fun parse(yaml: YamlNode): Result<String> {
        val string =
            when (val string = yaml.getNonNullOrEmptyString(name)) {
                is Error<String> -> return string
                is Success<String> -> string.data
            }

        return Success(string)
    }

    fun optional(default: String) = OptionalStringKeywordWithDefault(name, default, help)

    fun optional() = OptionalStringKeyword(name, help)

    override val optional = false
    override val default = null
    override val type = KeywordType.string
}

data class OptionalStringKeywordWithDefault(override val name: String, override val default: String, override val help: KeywordHelp) :
    SimpleKeyword<String> {

    fun parse(yaml: YamlNode): Result<String> {
        val string =
            when (val string = yaml.getOptionalString(name)) {
                is Error<String?> -> return Error<String>(string.error)
                is Success<String?> -> string.data ?: default
            }

        return Success(string)
    }

    override val optional = true
    override val type = KeywordType.string
}

data class OptionalStringKeyword(override val name: String, override val help: KeywordHelp) :
    SimpleKeyword<String> {

    fun parse(yaml: YamlNode): Result<String?> {
        val string =
            when (val string = yaml.getOptionalString(name)) {
                is Error<String?> -> return Error<String?>(string.error)
                is Success<String?> -> string.data
            }

        return Success(string)
    }
    override val optional = true
    override val default = null
    override val type = KeywordType.string
}

data class OptionalBooleanKeyword(
    override val name: String,
    override val help: KeywordHelp,
    override val default: Boolean,
) : SimpleKeyword<Boolean> {

    fun parse(yaml: YamlNode): Result<Boolean> {
        val bool =
            when (val result = yaml.getOptionalBoolean(name)) {
                is Error<Boolean?> -> return Error(result.error)
                is Success<Boolean?> -> {
                    result.data
                }
            }

        return Success(bool ?: default)
    }

    override val optional = true
    override val type = KeywordType.boolean
}
