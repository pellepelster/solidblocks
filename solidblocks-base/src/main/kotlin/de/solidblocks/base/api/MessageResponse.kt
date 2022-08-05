package de.solidblocks.base.api

import kotlin.reflect.KProperty1

fun String.messageResponses(attribute: KProperty1<*, *>? = null): List<MessageResponse> {
    if (attribute == null) {
        return listOf(MessageResponse(code = this))
    } else {
        return listOf(MessageResponse(attribute = attribute.name, code = this))
    }
}

data class MessageResponse(val attribute: String? = null, val code: String)
