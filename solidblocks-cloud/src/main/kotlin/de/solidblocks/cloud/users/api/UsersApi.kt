package de.solidblocks.cloud.users.api

import de.solidblocks.cloud.api.CloudApiHttpServer
import de.solidblocks.cloud.api.CloudApiHttpServer.Companion.JWT_ALGORITHM
import de.solidblocks.cloud.api.WhoamiResponse
import de.solidblocks.cloud.api.jsonRequest
import de.solidblocks.cloud.api.jsonResponse
import de.solidblocks.cloud.model.CloudRepository
import de.solidblocks.cloud.model.EnvironmentRepository
import de.solidblocks.cloud.model.UsersRepository
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.JWTOptions
import io.vertx.ext.web.RoutingContext

class UsersApi(
    val cloudApi: CloudApiHttpServer,
    val cloudRepository: CloudRepository,
    val environmentRepository: EnvironmentRepository,
    val usersRepository: UsersRepository
) {

    init {
        cloudApi.addUnprotectedRouter("/api/v1/users").post("/login").handler(this::login)
        cloudApi.addUnprotectedRouter("/api/v1/users").post("/register").handler(this::register)
        cloudApi.addRouter("/api/v1/users").get("/whoami").handler(this::whoami)
    }

    fun defaultUserEnvironment(): EnvironmentEntity? {

        val clouds = cloudRepository.listClouds()
        if (clouds.isEmpty()) {
            return null
        }

        val environments = environmentRepository.listEnvironments(clouds.first())
        return environments.firstOrNull()
    }

    fun register(rc: RoutingContext) {
        val request = rc.jsonRequest(RegisterRequest::class.java)

        val environment = defaultUserEnvironment()
    }

    fun login(rc: RoutingContext) {
        val request = rc.jsonRequest(LoginRequest::class.java)

        val token = cloudApi.authProvider.generateToken(
            JsonObject().put("email", "yyyy"),
            JWTOptions().setAlgorithm(JWT_ALGORITHM)
        )
        rc.jsonResponse(LoginResponse(token))
    }

    fun whoami(rc: RoutingContext) {

        val email = rc.user().principal().getString("email")

        rc.jsonResponse(WhoamiResponse(email))
    }
}
