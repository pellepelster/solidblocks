package de.solidblocks.core.utils

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

object JacksonUtils {

    val objectMapper = ObjectMapper()

    init {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        objectMapper.registerKotlinModule()
    }

    fun toMap(data: Any): Map<String, Any> {
        return objectMapper.convertValue(data, Map::class.java) as Map<String, Any>
    }
}
