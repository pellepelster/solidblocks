package de.solidblocks.cloud.utils

import com.charleskorn.kaml.*
import de.solidblocks.cloud.configuration.ConfigurationFactory
import de.solidblocks.cloud.configuration.PolymorphicConfigurationFactory

fun Location.logMessage() = "line ${this.line} colum ${this.column}"

fun YamlNode.isList(key: String) = (this.yamlMap.get<YamlNode>(key) is YamlList)

fun YamlNode.isMap(key: String) = (this.yamlMap.get<YamlNode>(key) is YamlMap)

fun YamlNode.getMapString(key: String) =
    if (this !is YamlMap) {
      null
    } else {
      this.yamlMap.entries
          .filter { it.key.content == key }
          .filter { it.value is YamlScalar }
          .map { it.value.yamlScalar.content }
          .firstOrNull()
    }

fun yamlParse(yaml: String) =
    try {
      Success(Yaml.default.parseToYamlNode(yaml))
    } catch (e: MalformedYamlException) {
      Error("invalid yaml document")
    } catch (e: EmptyYamlDocumentException) {
      YamlEmpty("yaml document is empty")
    }

fun YamlNode.getList(key: String) =
    if (this is YamlMap) {
      this.yamlMap.getList(key)
    } else {
      Error<YamlList>("expected a list at '$key' but got '${contentToString()}'")
    }

fun YamlNode.getMap(key: String) =
    if (this is YamlMap) {
      this.yamlMap.getMap(key)
    } else {
      Error<YamlMap>("expected a map at '$key' but got '${contentToString()}'")
    }

fun YamlMap.getList(key: String) =
    if (this.get<YamlNode>(key) == null) {
      YamlEmpty("no list found for key '$key' at ${this.location.logMessage()}")
    } else {
      if (!this.isList(key)) {
        Error("key '$key' should be a list ${location.logMessage()}")
      } else {
        Success(this.get<YamlList>(key)!!)
      }
    }

fun YamlMap.getMap(key: String) =
    if (this.get<YamlNode>(key) == null) {
      YamlEmpty("no map found for key '$key' at ${this.location.logMessage()}")
    } else {
      if (!this.isMap(key)) {
        Error("key '$key' should be a map ${location.logMessage()}")
      } else {
        Success(this.get<YamlMap>(key)!!)
      }
    }

fun YamlNode.getKeys(): Result<List<String>> =
    if (this is YamlMap) {
      this.yamlMap.getKeys()
    } else {
      Error("${contentToString()} is not a map")
    }

fun YamlMap.getKeys(): Result<List<String>> = Success(this.entries.map { it.key.content })

fun YamlNode.getScalar(key: String): YamlResult<String> {
  if (this !is YamlMap) {
    return Error("expected a map, got '${contentToString()}'")
  }

  val f = this.entries.filter { it.key.content == key }.map { it.value }.singleOrNull()

  if (f == null) {
    return YamlEmpty("key '$key' not found at ${this.location.logMessage()}")
  }

  if (f is YamlScalar) {
    if (f.content.isEmpty()) {
      return YamlEmpty("key '$key' is empty at ${this.location.logMessage()}")
    } else {
      return Success(f.content)
    }
  }

  if (f is YamlNull) {
    return YamlEmpty("key '$key' is null at ${this.location.logMessage()}")
  }

  return Error("expected string but found '${f.contentToString()}'")
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

fun YamlNode.getString(key: String): YamlResult<String> =
    when (val scalar = getScalar(key)) {
      is YamlEmpty<String> -> YamlEmpty(scalar.message)
      is Error<String> -> Error(scalar.error)
      is Success<String> -> Success(scalar.data)
    }

fun YamlNode.getNonNullOrEmptyString(key: String) = getNonNullOrEmptyScalar(key)

fun YamlNode.getOptionalString(key: String, default: String): Result<String> =
    when (val scalar = getScalar(key)) {
      is YamlEmpty<String> -> Success(default)
      is Error<String> -> Error<String>(scalar.error)
      is Success<String> -> Success(scalar.data)
    }

fun YamlNode.getOptionalString(key: String): Result<String?> =
    when (val scalar = getScalar(key)) {
      is YamlEmpty<String> -> Success<String?>(null)
      is Error<String> -> Error<String?>(scalar.error)
      is Success<String> -> Success(scalar.data)
    }

fun YamlNode.getOptionalBoolean(key: String): Result<Boolean?> =
    when (val scalar = getScalar(key)) {
      is YamlEmpty<String> -> Success<Boolean?>(null)
      is Error<String> -> Error<Boolean?>(scalar.error)
      is Success<String> ->
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

fun YamlNode.getBoolean(key: String): YamlResult<Boolean> =
    when (val scalar = getNonNullOrEmptyString(key)) {
      is Error<String> -> Error(scalar.error)
      is Success<String> ->
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

fun YamlNode.getNumber(key: String): YamlResult<Number?> =
    when (val scalar = getNonNullOrEmptyScalar(key)) {
      is Error<String> -> Error(scalar.error)
      is Success<String> ->
          try {
            Success(scalar.data.toInt())
          } catch (e: YamlScalarFormatException) {
            Error("expected number but got '${e.originalValue}' at ${this.location.logMessage()}")
          }
    }

fun YamlNode.getNumber(key: String, default: Number): Result<Number> =
    when (val result = getNumber(key)) {
      is YamlEmpty<Number?> -> Success(default)
      is Error<Number?> -> Error(result.error)
      is Success<Number?> -> Success(result.data ?: default)
    }

fun YamlNode.getBoolean(key: String, default: Boolean): Result<Boolean> =
    when (val result = getBoolean(key)) {
      is YamlEmpty<Boolean> -> Success(default)
      is Error<Boolean> -> Error(result.error)
      is Success<Boolean> -> Success(result.data ?: default)
    }

fun <T> YamlNode.getList(key: String, factory: ConfigurationFactory<T>): YamlResult<List<T>> {
  val list =
      when (val result = this.getList(key)) {
        is YamlEmpty<*> -> emptyList()
        is Error<*> -> return Error(result.error)
        is Success<YamlList> -> result.data.items.map { factory.parse(it) }
      }

  if (!list.all { it is Success<*> }) {
    return Error(list.filterIsInstance<Error<T>>().joinToString(", ") { it.error })
  }

  return Success(list.filterIsInstance<Success<T>>().map { it.data })
}

fun <T> YamlNode.getObject(key: String, factory: ConfigurationFactory<T>): Result<T> {
  val obj =
      when (val result = this.getMap(key)) {
        is YamlEmpty<*> -> factory.parse(YamlMap(emptyMap(), this.path))
        is Error<*> -> return Error(result.error)
        is Success<YamlMap> -> factory.parse(result.data)
      }

  return obj
}

fun <T> YamlNode.getPolymorphicList(
    key: String,
    factories: Map<String, PolymorphicConfigurationFactory<out T>>,
): YamlResult<List<T>> {
  val list =
      when (val result = this.getList(key)) {
        is YamlEmpty<*> -> emptyList()
        is Error<*> -> return Error(result.error)
        is Success<YamlList> ->
            result.data.items.map { listItem ->
              when (val type = listItem.getNonNullOrEmptyString("type")) {
                is Error<*> -> type
                is Success<*> -> {
                  val factory = factories.get(type.data)

                  factory?.parse(listItem)
                      ?: Error<T>(
                          "unknown type '${type.data}', possible types are ${
                                        factories.keys.map { "'$it'" }.joinToString(", ")
                                    } at ${listItem.location.logMessage()}",
                      )
                }
              }
            }
      }

  if (!list.all { it is Success<*> }) {
    return Error(list.filterIsInstance<Error<T>>().joinToString(", ") { it.error })
  }

  return Success(list.filterIsInstance<Success<T>>().map { it.data })
}
