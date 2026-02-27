package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getNonNullOrEmptyString
import de.solidblocks.cloud.utils.getOptionalString
import de.solidblocks.cloud.utils.logMessage

data class StringConstraints(val maxLength: Int? = null, val minLength: Int? = null, val regexPattern: String? = null, val options: List<String> = emptyList()) {
    companion object {
        val NONE = StringConstraints()
        val RFC_1123_NAME = StringConstraints(63, 2, "[a-z0-9]+(-[a-z0-9]+)*")
        val DOMAIN_NAME = StringConstraints(253, 4, "^([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}\$")
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

class StringKeywordOptional(name: String, constraints: StringConstraints, help: KeywordHelp) : BaseStringKeyword<String?>(name, constraints, true, null, help) {

    override fun parseInternal(yaml: YamlNode): Result<String?> {
        val string =
            when (val string = yaml.getOptionalString(name)) {
                is Error<String?> -> return Error<String?>(string.error)
                is Success<String?> -> string.data
            }

        return Success(string)
    }
}


class StringKeywordOptionalWithDefault(name: String, constraints: StringConstraints, default: String, help: KeywordHelp) : BaseStringKeyword<String>(name, constraints, true, default, help) {

    override fun parseInternal(yaml: YamlNode): Result<String?> {
        val string =
            when (val string = yaml.getOptionalString(name)) {
                is Error<String?> -> return Error<String?>(string.error)
                is Success<String?> -> string.data ?: default
            }

        return Success(string)
    }
}
