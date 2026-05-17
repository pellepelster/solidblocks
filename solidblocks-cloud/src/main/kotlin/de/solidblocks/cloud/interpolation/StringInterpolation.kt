package de.solidblocks.cloud.interpolation

import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success

fun collectInterpolatedStrings(template: String): List<String> {
    val interpolations = mutableListOf<String>()
    replaceInterpolatedStrings(template, {
        interpolations.add(it)
        Success("<none>")
    })

    return interpolations.toList()
}

fun replaceInterpolatedStrings(template: String, resolve: (String) -> Result<String>): Result<String> {
    val validationError = validateInterpolatedString(template)

    if (validationError != null) return Error(validationError)

    val result = StringBuilder()
    var i = 0

    while (i < template.length) {
        when {
            template[i] == '\\' && i + 1 < template.length && template[i + 1] == '$' -> {
                result.append('$')
                i += 2
            }

            template[i] == '$' && i + 1 < template.length && template[i + 1] == '{' -> {
                val end = template.indexOf('}', i + 2)
                val key = template.substring(i + 2, end)
                when (val resolved = resolve(key)) {
                    is Success -> result.append(resolved.data)
                    is Error -> return Error(resolved.error)
                }
                i = end + 1
            }

            else -> {
                result.append(template[i])
                i++
            }
        }
    }

    return Success(result.toString())
}

fun stringContainsInterpolation(template: String): Boolean {
    var i = 0
    while (i < template.length) {
        when {
            template[i] == '\\' && i + 1 < template.length && template[i + 1] == '$' -> i += 2
            template[i] == '$' && i + 1 < template.length && template[i + 1] == '{' -> return true
            else -> i++
        }
    }
    return false
}

fun validateInterpolatedString(template: String): String? {
    var i = 0
    var depth = 0

    while (i < template.length) {
        when {
            template[i] == '\\' && i + 1 < template.length && template[i + 1] == '$' -> i += 2
            template[i] == '$' && i + 1 < template.length && template[i + 1] == '{' -> {
                if (depth > 0) return "nested interpolation is not supported at position $i"
                depth++
                i += 2
            }

            template[i] == '{' && (i == 0 || template[i - 1] != '$') && depth == 0 -> i++
            template[i] == '}' && depth > 0 -> {
                depth--
                i++
            }

            else -> i++
        }
    }

    if (depth > 0) return "unclosed interpolation '\${' without closing '}'"
    return null
}
