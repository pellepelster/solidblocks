package de.solidblocks.cloud.auth.api

import de.solidblocks.cloud.users.api.UserResponse

data class WhoAmiResponse(val user: UserResponse? = null)
