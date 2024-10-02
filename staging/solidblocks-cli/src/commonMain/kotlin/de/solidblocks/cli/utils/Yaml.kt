package de.solidblocks.cli.utils

import com.charleskorn.kaml.Location
import com.charleskorn.kaml.MalformedYamlException
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.yamlMap
import com.charleskorn.kaml.yamlScalar

fun Location.logMessage() = "line ${this.line} colum ${this.column}"

fun YamlNode.isList(key: String) = (this.yamlMap.get<YamlNode>(key) is YamlList)

fun YamlNode.isMap(key: String) = (this.yamlMap.get<YamlNode>(key) is YamlMap)

fun YamlNode.getMapString(key: String) = if (this !is YamlMap) {
    null
} else {
    this.yamlMap.entries.filter { it.key.content == key }.filter { it.value is YamlScalar }
        .map { it.value.yamlScalar.content }.firstOrNull()
}

fun yamlParse(yaml: String) = try {
    Success(Yaml.default.parseToYamlNode(yaml))
} catch (e: MalformedYamlException) {
    Error("invalid yml")
}

fun YamlNode.getList(key: String) = if (this is YamlMap) {
    this.yamlMap.getList(key)
} else {
    Error("expected a map, got '${contentToString()}'")
}

fun YamlMap.getList(key: String) =
    if (this.get<YamlNode>(key) == null) {
        Empty("no list found at ${this.location.logMessage()}")
    } else {
        if (!this.isList(key)) {
            Error("'${key}' should be a list ${location.logMessage()}")
        } else {
            Success(this.get<YamlList>(key)!!)
        }
    }

fun YamlNode.getKeys() = if (this is YamlMap) {
    this.yamlMap.getKeys()
} else {
    Error("${contentToString()} is not a map")
}

fun YamlMap.getKeys() = Success(this.entries.map { it.key.content })

fun YamlNode.valueForKeyword(keyword: Keyword, fallback: Keyword) = when (val result = this.valueForKeyword(keyword)) {
    is Empty -> this.valueForKeyword(fallback)
    else -> result
}

fun YamlNode.valueForKeyword(keyword: Keyword): Result<String> {
    if (this !is YamlMap) {
        return Error("expected a map, got '${contentToString()}'")
    }

    val f = this.entries.filter { it.key.content == keyword.name }.map { it.value }.firstOrNull()

    if (f == null) {
        return Empty("keyword '${keyword}' not found at ${this.location.logMessage()}")
    }

    if (f is YamlScalar) {
        return Success(f.content)
    }

    if (f is YamlNull) {
        return Empty("value for keyword '${keyword}' is null at ${this.location.logMessage()}")
    }

    return Error("expected string but found '${f.contentToString()}'")
}