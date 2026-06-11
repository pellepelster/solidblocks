package de.solidblocks.cloud.configuration

import com.charleskorn.kaml.YamlNode
import de.solidblocks.cloud.utils.*

class NumberConstraints(val max: Int? = null, val min: Int? = null) {

    companion object {
        val NONE = NumberConstraints()
        val IP_PORTS = NumberConstraints(65536, 23)
        val VOLUME_SIZE = NumberConstraints(1024, 16)
    }
}

class NumberKeyword<T : Int?> internal constructor(
    override val name: String,
    override val help: KeywordHelp,
    val constraints: NumberConstraints,
    override val optional: Boolean,
    override val default: T?,
) : SimpleKeyword<T> {

    fun parse(yaml: YamlNode): Result<T> {
        val number =
            if (optional) {
                when (val result = yaml.getOptionalNumber(name)) {
                    is Error<Int?> -> return Error<T>(result.error)
                    is Success<Int?> -> result.data ?: default
                }
            } else {
                when (val result = yaml.getNumber(name)) {
                    is YamlEmpty<Number?> -> return Error<T>(result.message)
                    is Error<Number?> -> return Error<T>(result.error)
                    is Success<Number?> -> result.data?.toInt()
                }
            }

        if (number == null) {
            return Success<T>(number as T)
        }

        if (constraints.min != null && number < constraints.min) {
            return Error<T>(
                "'$name' may not be smaller than ${constraints.min} at ${yaml.location.logMessage()}",
            )
        }

        if (constraints.max != null && number > constraints.max) {
            return Error<T>(
                "'$name' may not be larger than ${constraints.max} at ${yaml.location.logMessage()}",
            )
        }

        return Success<T>(number as T)
    }

    override val type = KeywordType.number
}

fun NumberKeyword(name: String, help: KeywordHelp): NumberKeyword<Int> = NumberKeyword(name, help, NumberConstraints.NONE, optional = false, default = null)

fun <T : Int?> NumberKeyword<T>.constraints(constraints: NumberConstraints): NumberKeyword<T> = NumberKeyword(name, help, constraints, optional, default)

fun NumberKeyword<Int>.optional(): NumberKeyword<Int?> = NumberKeyword(name, help, constraints, optional = true, default = null)

fun NumberKeyword<Int>.default(value: Int): NumberKeyword<Int> = NumberKeyword(name, help, constraints, optional = true, default = value)
