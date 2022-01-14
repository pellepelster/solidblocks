package de.solidblocks.cloud.model.entities

import java.util.*

data class UserEntity(
    val id: UUID,
    val email: String,
    val salt: String,
    val password: String
)
