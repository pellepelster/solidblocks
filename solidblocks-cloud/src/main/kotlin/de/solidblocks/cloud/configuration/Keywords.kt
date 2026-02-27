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
import de.solidblocks.cloud.utils.getOptionalNumber
import de.solidblocks.cloud.utils.getOptionalString
import de.solidblocks.cloud.utils.getPolymorphicList
import de.solidblocks.cloud.utils.logMessage

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

data class StringConstraints(val maxLength: Int? = null, val minLength: Int? = null, val regexPattern: String? = null, val options: List<String> = emptyList()) {
    companion object {
        val NONE = StringConstraints()
        val RFC_1123_NAME = StringConstraints(63, 2, "[a-z0-9]+(-[a-z0-9]+)*")
        val DOMAIN_NAME = StringConstraints(253, 4, "^([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}\$")
        val HETZNER_INSTANCE_TYPE = StringConstraints(
            options = listOf(
                "cpx21",
                "cpx31",
                "cpx41",
                "cpx51",
                "cax11",
                "cax21",
                "cax31",
                "cax41",
                "ccx13",
                "ccx23",
                "ccx33",
                "ccx43",
                "ccx53",
                "ccx63",
                "cpx12",
                "cpx22",
                "cpx32",
                "cpx42",
                "cpx52",
                "cpx62",
                "cx23",
                "cx33",
                "cx43",
                "cx53"
            )
        )
        val HETZNER_LOCATIONS = StringConstraints(options = listOf("fsn1", "nbg1", "hel1", "ash", "hil", "sin"))
    }
}

abstract class BaseStringKeyword<T>(override val name: String, val constraints: StringConstraints, override val optional: Boolean, override val default: T?, override val help: KeywordHelp) : SimpleKeyword<T> {

    abstract fun parseInternal(yaml: YamlNode): Result<String?>

    open fun parse(yaml: YamlNode): Result<T> {
        val string =
            when (val result = parseInternal(yaml)) {
                is Error<String?> -> return Error<T>(result.error)
                is Success<String?> -> {
                    result.data
                }
            }

        if (string == null) {
            return Success<T>(string as T)
        }

        if (constraints.maxLength != null && string.length > constraints.maxLength) {
            return Error<T>("maximum allowed length for '${name}' is ${constraints.maxLength} characters at ${yaml.location.logMessage()}")
        }

        if (constraints.minLength != null && string.length < constraints.minLength) {
            return Error<T>("'${name}' should be at least ${constraints.minLength} characters long at ${yaml.location.logMessage()}")
        }

        if (constraints.regexPattern != null) {
            if (!constraints.regexPattern.toRegex().matches(string)) {
                return Error<T>("'${name}' must match '${constraints.regexPattern}' at ${yaml.location.logMessage()}")
            }
        }

        if (constraints.options.isNotEmpty() && !constraints.options.contains(string)) {
            return Error<T>("'${string}' is not allowed for '${name}', possible options are: ${constraints.options.joinToString(", ") { "'$it'" }} at ${yaml.location.logMessage()}")
        }

        return Success<T>(string as T)
    }

    override val type = KeywordType.string
}

class StringKeyword(name: String, constraints: StringConstraints, help: KeywordHelp) : BaseStringKeyword<String>(name, constraints, false, null, help) {

    override fun parseInternal(yaml: YamlNode): Result<String?> = when (val result = yaml.getNonNullOrEmptyString(name)) {
        is Error<String> -> Error<String?>(result.error)
        is Success<String> -> Success<String?>(result.data)
    }
}

class OptionalStringKeyword(name: String, constraints: StringConstraints, help: KeywordHelp) : BaseStringKeyword<String?>(name, constraints, true, null, help) {

    override fun parseInternal(yaml: YamlNode): Result<String?> {
        val string =
            when (val string = yaml.getOptionalString(name)) {
                is Error<String?> -> return Error<String?>(string.error)
                is Success<String?> -> string.data
            }

        return Success(string)
    }
}


class OptionalStringKeywordWithDefault(name: String, constraints: StringConstraints, default: String, help: KeywordHelp) : BaseStringKeyword<String>(name, constraints, true, default, help) {

    override fun parseInternal(yaml: YamlNode): Result<String?> {
        val string =
            when (val string = yaml.getOptionalString(name)) {
                is Error<String?> -> return Error<String?>(string.error)
                is Success<String?> -> string.data ?: default
            }

        return Success(string)
    }
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

data class OptionalNumberKeyword(
    override val name: String,
    override val help: KeywordHelp,
    override val default: Int,
) : SimpleKeyword<Int> {

    fun parse(yaml: YamlNode): Result<Int> {
        val bool =
            when (val result = yaml.getOptionalNumber(name)) {
                is Error<Int?> -> return Error(result.error)
                is Success<Int?> -> {
                    result.data
                }
            }

        return Success(bool ?: default)
    }

    override val optional = true
    override val type = KeywordType.boolean
}
