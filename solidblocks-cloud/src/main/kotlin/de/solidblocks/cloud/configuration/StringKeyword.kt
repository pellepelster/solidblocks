package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.KeywordHelp
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.cloud.utils.getNonNullOrEmptyString
import de.solidblocks.cloud.utils.getOptionalString
import de.solidblocks.cloud.utils.logMessage

data class StringConstraints(val maxLength: Int? = null, val minLength: Int? = null, val regexPattern: String? = null, val options: List<String> = emptyList()) {
    companion object {
        val NONE = StringConstraints()
        val RFC_1123_NAME = StringConstraints(63, 2, "[a-z0-9]+(-[a-z0-9]+)*")
        val DOMAIN_NAME =
            StringConstraints(253, 4, "^([a-z0-9]+(-[a-z0-9]+)*)(\\.([a-z0-9]+(-[a-z0-9]+)*))*\$")
    }
}

class StringKeyword<T : String?> internal constructor(
    override val name: String,
    override val help: KeywordHelp,
    val constraints: StringConstraints,
    override val optional: Boolean,
    override val default: T?,
) : SimpleKeyword<T> {

    fun parse(yaml: YamlNode): Result<T> {
        val string =
            if (optional) {
                when (val result = yaml.getOptionalString(name)) {
                    is Error<String?> -> return Error<T>(result.error)
                    is Success<String?> -> result.data ?: default
                }
            } else {
                when (val result = yaml.getNonNullOrEmptyString(name)) {
                    is Error<String> -> return Error<T>(result.error)
                    is Success<String> -> result.data
                }
            }

        if (string == null) {
            return Success<T>(string as T)
        }

        if (constraints.maxLength != null && string.length > constraints.maxLength) {
            return Error<T>(
                "maximum allowed length for '$name' is ${constraints.maxLength} characters at ${yaml.location.logMessage()}",
            )
        }

        if (constraints.minLength != null && string.length < constraints.minLength) {
            return Error<T>(
                "'$name' should be at least ${constraints.minLength} characters long at ${yaml.location.logMessage()}",
            )
        }

        if (constraints.regexPattern != null) {
            if (!constraints.regexPattern.toRegex().matches(string)) {
                return Error<T>(
                    "'$name' must match '${constraints.regexPattern}' at ${yaml.location.logMessage()}",
                )
            }
        }

        if (constraints.options.isNotEmpty() && !constraints.options.contains(string)) {
            return Error<T>(
                "'$string' is not allowed for '$name', possible options are: ${constraints.options.joinToString(", ") { "'$it'" }} at ${yaml.location.logMessage()}",
            )
        }

        return Success<T>(string as T)
    }

    override val type = KeywordType.string
}

fun StringKeyword(name: String, help: KeywordHelp): StringKeyword<String> = StringKeyword(name, help, StringConstraints.NONE, optional = false, default = null)

fun <T : String?> StringKeyword<T>.constraints(constraints: StringConstraints): StringKeyword<T> = StringKeyword(name, help, constraints, optional, default)

fun StringKeyword<String>.optional(): StringKeyword<String?> = StringKeyword(name, help, constraints, optional = true, default = null)

fun StringKeyword<String>.default(value: String): StringKeyword<String> = StringKeyword(name, help, constraints, optional = true, default = value)
