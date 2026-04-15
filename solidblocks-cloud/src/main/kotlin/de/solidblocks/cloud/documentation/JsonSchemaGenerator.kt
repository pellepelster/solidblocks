package de.solidblocks.cloud.documentation

import de.solidblocks.cloud.configuration.*
import kotlinx.serialization.json.*

private val prettyJson = Json { prettyPrint = true }

class JsonSchemaGenerator {

    private val definitions = mutableMapOf<String, JsonObject>()

    fun generateJsonSchema(factory: ConfigurationFactory<*>): String {
        definitions.clear()
        val rootSchema = buildObjectSchema(factory)

        val result = buildJsonObject {
            put("\$schema", "https://json-schema.org/draft/2020-12/schema")
            put("title", factory.help.title)
            put("description", factory.help.description)
            rootSchema.forEach { (key, value) -> put(key, value) }
            if (definitions.isNotEmpty()) {
                putJsonObject("definitions") {
                    definitions.forEach { (key, value) -> put(key, value) }
                }
            }
        }

        return prettyJson.encodeToString(JsonElement.serializer(), result)
    }

    private fun buildObjectSchema(factory: ConfigurationFactory<*>): JsonObject {
        val required = factory.keywords
            .filterIsInstance<SimpleKeyword<*>>()
            .filter { !it.optional }
            .map { it.name }

        return buildJsonObject {
            put("type", "object")
            put("additionalProperties", false)
            if (required.isNotEmpty()) {
                put("required", JsonArray(required.map { JsonPrimitive(it) }))
            }
            putJsonObject("properties") {
                factory.keywords.forEach { keyword ->
                    put(keyword.name, keywordToSchema(keyword))
                }
            }
        }
    }

    private fun buildPolymorphicObjectSchema(factory: ConfigurationFactory<*>, typeKey: String): JsonObject {
        val required = mutableListOf("type")
        required.addAll(
            factory.keywords
                .filterIsInstance<SimpleKeyword<*>>()
                .filter { !it.optional }
                .map { it.name },
        )

        return buildJsonObject {
            put("type", "object")
            put("title", factory.help.title)
            put("description", factory.help.description)
            put("additionalProperties", false)
            put("required", JsonArray(required.map { JsonPrimitive(it) }))
            putJsonObject("properties") {
                putJsonObject("type") {
                    put("type", "string")
                    put("const", typeKey)
                }
                factory.keywords.forEach { keyword ->
                    put(keyword.name, keywordToSchema(keyword))
                }
            }
        }
    }

    private fun keywordToSchema(keyword: Keyword<*>): JsonObject = when (keyword) {
        is SimpleKeyword<*> -> simpleKeywordToSchema(keyword)
        is PolymorphicListKeyword<*> -> polymorphicListKeywordToSchema(keyword)
        is ListKeyword<*> -> listKeywordToSchema(keyword)
        is ObjectKeyword<*> -> objectKeywordToSchema(keyword)
    }

    private fun simpleKeywordToSchema(keyword: SimpleKeyword<*>): JsonObject = buildJsonObject {
        put("description", keyword.help.description)
        when (keyword) {
            is BaseStringKeyword<*> -> {
                if (keyword.constraints.options.isNotEmpty()) {
                    put("type", "string")
                    put("enum", JsonArray(keyword.constraints.options.map { JsonPrimitive(it) }))
                } else {
                    put("type", "string")
                    keyword.constraints.minLength?.let { put("minLength", it) }
                    keyword.constraints.maxLength?.let { put("maxLength", it) }
                    keyword.constraints.regexPattern?.let { put("pattern", it) }
                }
                keyword.default?.let { put("default", it.toString()) }
            }

            is BaseNumberKeyword<*> -> {
                put("type", "integer")
                keyword.constraints.min?.let { put("minimum", it) }
                keyword.constraints.max?.let { put("maximum", it) }
                (keyword.default as? Int)?.let { put("default", it) }
            }

            is OptionalBooleanKeyword -> {
                put("type", "boolean")
                put("default", keyword.default)
            }
        }
    }

    private fun listKeywordToSchema(keyword: ListKeyword<*>): JsonObject = buildJsonObject {
        put("description", keyword.help.description)
        put("type", "array")
        if (keyword is StringListKeyword) {
            putJsonObject("items") { put("type", "string") }
        } else {
            put("items", buildObjectSchema(keyword.factory))
        }
    }

    private fun polymorphicListKeywordToSchema(keyword: PolymorphicListKeyword<*>): JsonObject {
        keyword.factories.forEach { (typeKey, factory) ->
            val defName = factory.help.title.replace(" ", "")
            if (!definitions.containsKey(defName)) {
                definitions[defName] = buildPolymorphicObjectSchema(factory, typeKey)
            }
        }

        return buildJsonObject {
            put("description", keyword.help.description)
            put("type", "array")
            putJsonObject("items") {
                put(
                    "oneOf",
                    JsonArray(
                        keyword.factories.map { (_, factory) ->
                            val defName = factory.help.title.replace(" ", "")
                            buildJsonObject { put("\$ref", "#/definitions/$defName") }
                        },
                    ),
                )
            }
        }
    }

    private fun objectKeywordToSchema(keyword: ObjectKeyword<*>): JsonObject = buildJsonObject {
        put("description", keyword.help.description)
        buildObjectSchema(keyword.factory).forEach { (key, value) -> put(key, value) }
    }
}
