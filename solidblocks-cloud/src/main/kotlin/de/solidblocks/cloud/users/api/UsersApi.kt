package de.solidblocks.cloud.users.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.CloudApiHttpServer.Companion.JWT_ALGORITHM
import de.solidblocks.cloud.api.WhoAmiResponse
import de.solidblocks.cloud.api.jsonRequest
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.users.UsersManager
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.web.RoutingContext

class UsersApi(
    val cloudApi: CloudApiHttpServer,
    val usersManager: UsersManager
) {

    init {
    }

}
