package de.solidblocks.base.api

import kotlin.reflect.KProperty1

fun String.messageResponses(attribute: KProperty1<*, *>) = listOf(MessageResponse(attribute = attribute.name, code = this))

data class MessageResponse(val attribute: String? = null, val code: String)
