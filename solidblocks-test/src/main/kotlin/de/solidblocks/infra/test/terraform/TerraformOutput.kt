package de.solidblocks.infra.test.terraform

import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

data class TerraformOutput(val output: Map<String, OutputVariable>) {
  fun raw() = output.mapValues { it.value.value }

  private fun getPrimitive(name: String): JsonPrimitive {
    if (output[name]?.value is JsonPrimitive) {
      return output[name]!!.value as JsonPrimitive
    }

    throw RuntimeException("key '$name' not found")
  }

  private fun getOptionalPrimitive(name: String): JsonPrimitive? {
    if (output[name]?.value is JsonPrimitive) {
      return output[name]?.value as JsonPrimitive
    }

    return null
  }

  fun getString(name: String) = getPrimitive(name).content

  fun getOptionalString(name: String) = getOptionalPrimitive(name)?.content

  fun getNumber(name: String) = getPrimitive(name).content.toLong()

  fun getBoolean(name: String) = getPrimitive(name).content.toBoolean()

  @OptIn(InternalSerializationApi::class)
  fun <T : Any> getList(name: String, kClass: KClass<T>): List<T> {
    if (output[name] != null) {
      return Json.decodeFromJsonElement(
          ListSerializer(kClass.serializer()),
          output[name]!!.value!!,
      )
    }

    throw RuntimeException("key '$name' not found")
  }

  @OptIn(InternalSerializationApi::class)
  fun <T : Any> getObject(name: String, kClass: KClass<T>): T {
    if (output[name] != null) {
      return Json.decodeFromJsonElement(
          kClass.serializer(),
          output[name]!!.value!!,
      )
    }

    throw RuntimeException("key '$name' not found")
  }
}
