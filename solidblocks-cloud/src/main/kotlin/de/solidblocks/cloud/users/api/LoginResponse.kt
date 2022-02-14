package de.solidblocks.cloud.users.api

import de.solidblocks.base.api.MessageResponse

data class LoginResponse(
    val token: String? = null,
    val user: UserResponse? = null,
    val messages: List<MessageResponse> = emptyList()
)
