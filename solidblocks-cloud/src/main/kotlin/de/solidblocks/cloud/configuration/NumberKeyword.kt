package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.utils.*

class NumberConstraints(val max: Int? = null, val min: Int? = null) {

    companion object {
        val NONE = NumberConstraints()
        val IP_PORTS = NumberConstraints(65536, 23)
    }
}

abstract class BaseNumberKeyword<T>(override val name: String, val constraints: NumberConstraints, override val optional: Boolean, override val default: T?, override val help: KeywordHelp) : SimpleKeyword<T> {

    abstract fun parseInternal(yaml: YamlNode): Result<Int?>

    open fun parse(yaml: YamlNode): Result<T> {
        val number =
            when (val result = parseInternal(yaml)) {
                is Error<Int?> -> return Error<T>(result.error)
                is Success<Int?> -> {
                    result.data
                }
            }

        if (number == null) {
            return Success(number as T)
        }

        if (constraints.min != null && number < constraints.min) {
            return Error<T>("'${name}' may not be smaller then ${constraints.min} at ${yaml.location.logMessage()}")
        }

        if (constraints.max != null && number > constraints.max) {
            return Error<T>("'${name}' may not be larger then ${constraints.min} at ${yaml.location.logMessage()}")
        }

        return Success(number as T)
    }

    override val type = KeywordType.number
}


class NumberKeyword(
    name: String,
    constraints: NumberConstraints,
    help: KeywordHelp,
) : BaseNumberKeyword<Int>(name, constraints, true, null, help) {

    override fun parseInternal(yaml: YamlNode): Result<Int?> {
        val number =
            when (val result = yaml.getOptionalNumber(name)) {
                is Error<Int?> -> return Error<Int?>(result.error)
                is Success<Int?> -> result.data

            }

        return Success(number ?: default)
    }

}

class NumberKeywordOptional(
    name: String,
    constraints: NumberConstraints,
    help: KeywordHelp,
) : BaseNumberKeyword<Int>(name, constraints, true, null, help) {

    override fun parseInternal(yaml: YamlNode): Result<Int?> {
        val number =
            when (val result = yaml.getOptionalNumber(name)) {
                is Error<Int?> -> return Error<Int?>(result.error)
                is Success<Int?> -> result.data

            }

        return Success(number ?: default)
    }

}

class NumberKeywordOptionalWithDefault(
    name: String,
    constraints: NumberConstraints,
    help: KeywordHelp,
    default: Int,
) : BaseNumberKeyword<Int>(name, constraints, true, default, help) {

    override fun parseInternal(yaml: YamlNode): Result<Int?> {
        val number =
            when (val result = yaml.getOptionalNumber(name)) {
                is Error<Int?> -> return Error<Int?>(result.error)
                is Success<Int?> -> result.data

            }

        return Success(number ?: default)
    }

}
