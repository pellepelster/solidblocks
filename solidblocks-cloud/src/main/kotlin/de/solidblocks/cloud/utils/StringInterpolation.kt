package de.solidblocks.cloud.utils

data class StringInterpolationResult(val value: String?, val error: String?)

fun interpolateString(template: String, resolve: (String) -> String): StringInterpolationResult {
    val validation = validateInterpolationTemplate(template)
    if (validation != null) return StringInterpolationResult(null, validation)

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
                result.append(resolve(key))
                i = end + 1
            }
            else -> {
                result.append(template[i])
                i++
            }
        }
    }

    return StringInterpolationResult(result.toString(), null)
}

fun containsInterpolation(template: String): Boolean {
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

fun validateInterpolationTemplate(template: String): String? {
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
