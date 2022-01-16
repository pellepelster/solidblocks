package de.solidblocks.cloud.api

fun String.toMessages() = listOf(MessageResponse(this))

data class MessageResponse(val code: String)
