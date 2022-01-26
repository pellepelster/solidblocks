package de.solidblocks.cloud.auth.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.CloudApiHttpServer.Companion.JWT_ALGORITHM
import de.solidblocks.cloud.api.MessageResponse
import de.solidblocks.cloud.api.jsonRequest
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.ErrorCodes
import de.solidblocks.cloud.users.UsersManager
import de.solidblocks.cloud.users.api.LoginRequest
import de.solidblocks.cloud.users.api.LoginResponse
import de.solidblocks.cloud.users.api.UserResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.web.RoutingContext

class AuthApi(
    val cloudApi: CloudApiHttpServer,
    val cloudRepository: CloudRepository,
    val environmentRepository: EnvironmentRepository,
    val usersManager: UsersManager
) {

    init {
        cloudApi.configureSubRouter("/api/v1/auth", configure = { router, authHandler ->
            router.post("/login").handler(this::login)
            router.get("/whoami").handler(authHandler).handler(this::whoami)
        })
    }

    private fun login(rc: RoutingContext) {
        val request = rc.jsonRequest(LoginRequest::class.java)

        val user = usersManager.loginUser(request.email, request.password)

        if (user == null) {
            rc.jsonResponse(
                LoginResponse(messages = listOf(MessageResponse(code = ErrorCodes.LOGIN.INVALID_CREDENTIALS))),
                401
            )
            return
        }

        val token = cloudApi.authProvider.generateToken(
            JsonObject().put("email", user.email)
                .put("scope", user.scope()),
            JWTOptions().setAlgorithm(JWT_ALGORITHM)
        )

        rc.jsonResponse(LoginResponse(token, UserResponse(user.email, user.scope())))
    }

    private fun whoami(rc: RoutingContext) {
        val email = rc.user().principal().getString("email")
        val user = usersManager.getUser(email)

        if (user == null) {
            rc.jsonResponse(401)
            return
        }

        rc.jsonResponse(WhoAmiResponse(UserResponse(user.email, user.scope())))
    }
}
