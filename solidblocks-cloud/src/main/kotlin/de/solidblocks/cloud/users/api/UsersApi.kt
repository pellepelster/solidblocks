package de.solidblocks.cloud.users.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.users.UsersManager

class UsersApi(
    val cloudApi: CloudApiHttpServer,
    val usersManager: UsersManager
) {

    init {
    }

}
