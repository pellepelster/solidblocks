package de.solidblocks.ingress.agent.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class CaddyConfig(val apps: Map<String, Any> = emptyMap(), val admin: Admin = Admin()) {
    companion object {
        private val objectMapper = jacksonObjectMapper()

        fun serialize(config: CaddyConfig): String = objectMapper.writeValueAsString(config)
        fun serializeToBytes(config: CaddyConfig): ByteArray = objectMapper.writeValueAsBytes(config)
    }
}
