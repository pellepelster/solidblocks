package de.solidblocks.cloud.utils

import com.charleskorn.kaml.*
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory
import kotlin.collections.emptyMap

fun Location.logMessage() = "line ${this.line} column ${this.column}"

fun YamlNode.isList(key: String) = (this.yamlMap.get<YamlNode>(key) is YamlList)

fun YamlNode.isMap(key: String) = (this.yamlMap.get<YamlNode>(key) is YamlMap)

fun YamlNode.getMapString(key: String) = if (this !is YamlMap) {
    null
} else {
    this.yamlMap.entries
        .filter { it.key.content == key }
        .filter { it.value is YamlScalar }
        .map { it.value.yamlScalar.content }
        .firstOrNull()
}

fun yamlParse(yaml: String): YamlResult<YamlNode> = try {
    YamlSuccess(Yaml.default.parseToYamlNode(yaml))
} catch (e: MalformedYamlException) {
    YamlError("invalid yaml document")
} catch (e: EmptyYamlDocumentException) {
    YamlEmpty("yaml document is empty")
}

fun YamlNode.getList(key: String) = if (this is YamlMap) {
    this.yamlMap.getList(key)
} else {
    YamlError<YamlList>("expected a list at '$key' but got '${contentToString()}'")
}

fun YamlNode.getMap(key: String) = if (this is YamlMap) {
    this.yamlMap.getMap(key)
} else {
    YamlError<YamlMap>("expected a map at '$key' but got '${contentToString()}'")
}

fun YamlNode.getStringMap(key: String): YamlResult<Map<String, String>> = if (this is YamlMap) {
    when (val map = this.yamlMap.getMap(key)) {
        is YamlError<YamlMap> -> YamlError<Map<String, String>>(map.error)
        is YamlSuccess<YamlMap> -> {
            val nonScalarKeys = map.data.entries.filter {
                it.value !is YamlScalar
            }.map { it.key }

            if (nonScalarKeys.isNotEmpty()) {
                YamlError<Map<String, String>>("found non string value in map '$key' at key(s) ${nonScalarKeys.joinToString(", ") { it.content }}")
            } else {
                YamlSuccess(
                    map.data.entries.map {
                        it.key.content to (it.value as YamlScalar).content
                    }.associate { it },
                )
            }
        }

        is YamlEmpty<YamlMap> -> YamlEmpty("'$key' not set")
    }
} else {
    YamlError<Map<String, String>>("expected a map at '$key' but got '${contentToString()}'")
}

fun YamlMap.getList(key: String) = if (this.get<YamlNode>(key) == null) {
    YamlEmpty("no list found for key '$key' at ${this.location.logMessage()}")
} else {
    if (!this.isList(key)) {
        YamlError("key '$key' should be a list ${location.logMessage()}")
    } else {
        YamlSuccess(this.get<YamlList>(key)!!)
    }
}

fun YamlMap.getMap(key: String) = if (this.get<YamlNode>(key) == null) {
    YamlEmpty("no map found for key '$key' at ${this.location.logMessage()}")
} else {
    if (!this.isMap(key)) {
        YamlError("key '$key' should be a map ${location.logMessage()}")
    } else {
        YamlSuccess(this.get<YamlMap>(key)!!)
    }
}

fun YamlNode.getKeys(): Result<List<String>> = if (this is YamlMap) {
    this.yamlMap.getKeys()
} else {
    Error("${contentToString()} is not a map")
}

fun YamlMap.getKeys(): Result<List<String>> = Success(this.entries.map { it.key.content })

fun YamlNode.getScalar(key: String): YamlResult<String> {
    if (this !is YamlMap) {
        return YamlError("expected a map, got '${contentToString()}'")
    }

    val f = this.entries.filter { it.key.content == key }.map { it.value }.singleOrNull()

    if (f == null) {
        return YamlEmpty("key '$key' not found at ${this.location.logMessage()}")
    }

    if (f is YamlScalar) {
        if (f.content.isEmpty()) {
            return YamlEmpty("key '$key' is empty at ${this.location.logMessage()}")
        } else {
            return YamlSuccess(f.content)
        }
    }

    if (f is YamlNull) {
        return YamlEmpty("key '$key' is null at ${this.location.logMessage()}")
    }

    return YamlError("expected string but found '${f.contentToString()}'")
}

fun YamlNode.getNonNullOrEmptyScalar(key: String): Result<String> {
    if (this !is YamlMap) {
        return Error("expected a map, got '${contentToString()}'")
    }

    val f = this.entries.filter { it.key.content == key }.map { it.value }.singleOrNull()

    if (f == null) {
        return Error("key '$key' not found at ${this.location.logMessage()}")
    }

    if (f is YamlScalar) {
        if (f.content.isEmpty()) {
            return Error("key '$key' is empty at ${this.location.logMessage()}")
        } else {
            return Success(f.content)
        }
    }

    if (f is YamlNull) {
        return Error("key '$key' is null at ${this.location.logMessage()}")
    }

    return Error("expected string but found '${f.contentToString()}'")
}

fun YamlNode.getString(key: String): YamlResult<String> = when (val scalar = getScalar(key)) {
    is YamlEmpty<String> -> YamlEmpty(scalar.message)
    is YamlError<String> -> YamlError(scalar.error)
    is YamlSuccess<String> -> YamlSuccess(scalar.data)
}

fun YamlNode.getNonNullOrEmptyString(key: String) = getNonNullOrEmptyScalar(key)

fun YamlNode.getOptionalString(key: String, default: String): Result<String> = when (val scalar = getScalar(key)) {
    is YamlEmpty<String> -> Success(default)
    is YamlError<String> -> Error<String>(scalar.error)
    is YamlSuccess<String> -> Success(scalar.data)
}

fun YamlNode.getOptionalString(key: String): Result<String?> = when (val scalar = getScalar(key)) {
    is YamlEmpty<String> -> Success<String?>(null)
    is YamlError<String> -> Error<String?>(scalar.error)
    is YamlSuccess<String> -> Success(scalar.data)
}

fun YamlNode.getOptionalBoolean(key: String): Result<Boolean?> = when (val scalar = getScalar(key)) {
    is YamlEmpty<String> -> Success<Boolean?>(null)
    is YamlError<String> -> Error<Boolean?>(scalar.error)
    is YamlSuccess<String> ->
        when (scalar.data) {
            "true" -> Success(true)
            "false" -> Success(false)
            else -> {
                Error(
                    "expected 'true' or 'false' but got '${scalar.data}' at ${this.location.logMessage()}",
                )
            }
        }
}

fun YamlNode.getBoolean(key: String): YamlResult<Boolean> = when (val scalar = getNonNullOrEmptyString(key)) {
    is Error<String> -> YamlError(scalar.error)
    is Success<String> ->
        when (scalar.data) {
            "true" -> YamlSuccess(true)
            "false" -> YamlSuccess(false)
            else -> {
                YamlError(
                    "expected 'true' or 'false' but got '${scalar.data}' at ${this.location.logMessage()}",
                )
            }
        }
}

fun YamlNode.getOptionalNumber(key: String): Result<Int?> = when (val scalar = getScalar(key)) {
    is YamlEmpty<String> -> Success<Int?>(null)
    is YamlError<String> -> Error<Int?>(scalar.error)
    is YamlSuccess<String> -> {
        try {
            Success(scalar.data.toInt())
        } catch (e: NumberFormatException) {
            Error("expected number but got '${scalar.data}' at ${this.location.logMessage()}")
        }
    }
}

fun YamlNode.getNumber(key: String): YamlResult<Number?> = when (val scalar = getNonNullOrEmptyScalar(key)) {
    is Error<String> -> YamlError(scalar.error)
    is Success<String> ->
        try {
            YamlSuccess(scalar.data.toInt())
        } catch (e: NumberFormatException) {
            YamlError("expected number but got '${scalar.data}' at ${this.location.logMessage()}")
        }
}

fun YamlNode.getNumber(key: String, default: Number): Result<Number> = when (val result = getOptionalNumber(key)) {
    is Error<Int?> -> Error(result.error)
    is Success<Int?> -> Success(result.data ?: default)
}

fun YamlNode.getBoolean(key: String, default: Boolean): Result<Boolean> = when (val result = getOptionalBoolean(key)) {
    is Error<Boolean?> -> Error(result.error)
    is Success<Boolean?> -> Success(result.data ?: default)
}

fun <T> YamlNode.getList(key: String, factory: ConfigurationFactory<T>): YamlResult<List<T>> {
    val list =
        when (val result = this.getList(key)) {
            is YamlEmpty<*> -> emptyList()
            is YamlError<*> -> return YamlError(result.error)
            is YamlSuccess<YamlList> -> result.data.items.map { factory.parse(it) }
        }

    if (!list.all { it is Success<*> }) {
        return YamlError(list.filterIsInstance<Error<T>>().joinToString(", ") { it.error })
    }

    return YamlSuccess(list.filterIsInstance<Success<T>>().map { it.data })
}

fun <T> YamlNode.getObject(key: String, factory: ConfigurationFactory<T>): Result<T> {
    val obj =
        when (val result = this.getMap(key)) {
            is YamlEmpty<*> -> factory.parse(YamlMap(emptyMap(), this.path))
            is YamlError<*> -> return Error(result.error)
            is YamlSuccess<YamlMap> -> factory.parse(result.data)
        }

    return obj
}

fun <T> YamlNode.getPolymorphicList(key: String, factories: Map<String, PolymorphicConfigurationFactory<out T>>): YamlResult<List<T>> {
    val list =
        when (val result = this.getList(key)) {
            is YamlEmpty<*> -> emptyList()
            is YamlError<*> -> return YamlError(result.error)
            is YamlSuccess<YamlList> ->
                result.data.items.map { listItem ->
                    when (val type = listItem.getNonNullOrEmptyString("type")) {
                        is Error<*> -> type
                        is Success<*> -> {
                            val factory = factories[type.data]

                            factory?.parse(listItem)
                                ?: Error<T>(
                                    "unknown type '${type.data}', possible types are ${
                                        factories.keys.joinToStringOrEmpty(", ") { "'$it'" }
                                    } at ${listItem.location.logMessage()}",
                                )
                        }
                    }
                }
        }

    if (!list.all { it is Success<*> }) {
        return YamlError(list.filterIsInstance<Error<T>>().joinToString(", ") { it.error })
    }

    return YamlSuccess(list.filterIsInstance<Success<T>>().map { it.data })
}
