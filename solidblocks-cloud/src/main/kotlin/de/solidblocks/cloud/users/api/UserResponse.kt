package de.solidblocks.cloud.users.api

import de.solidblocks.cloud.model.entities.Scope

data class UserResponse(val email: String, val scope: Scope)