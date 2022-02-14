package de.solidblocks.cloud.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.base.api.MessageResponse
import de.solidblocks.cloud.model.ErrorCodes
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.ext.auth.PubSecKeyOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.JWTAuthHandler
import mu.KotlinLogging

val jackson = jacksonObjectMapper()

fun RoutingContext.jsonResponse(response: Any, code: Int = 200) {
    this.response().setStatusCode(code).putHeader("Content-Type", "application/json").end(jackson.writeValueAsString(response))
}

fun RoutingContext.jsonResponse(code: Int = 200) {
    this.response().setStatusCode(code).end()
}

fun <T> RoutingContext.jsonRequest(clazz: Class<T>): T {
    return jackson.readValue(this.body.bytes, clazz)
}

class CloudApiHttpServer(val privateKey: String, val publicKey: String, port: Int = 8080) {

    companion object {
        const val JWT_ALGORITHM = "RS512"
    }

    private val logger = KotlinLogging.logger {}

    private val vertx: Vertx

    private var server: HttpServer

    private val router: Router

    val authProvider: JWTAuth

    val authHandler: JWTAuthHandler

    val listen: Future<HttpServer>

    val port: Int
        get() = server.actualPort()

    init {
        jackson.setSerializationInclusion(JsonInclude.Include.NON_NULL)

        vertx = Vertx.vertx()
        router = Router.router(vertx)
        server = vertx.createHttpServer()

        authProvider = JWTAuth.create(
            vertx,
            JWTAuthOptions()
                .addPubSecKey(
                    PubSecKeyOptions()
                        .setAlgorithm(JWT_ALGORITHM)
                        .setBuffer(Buffer.buffer(privateKey))
                )
                .addPubSecKey(
                    PubSecKeyOptions()
                        .setAlgorithm(JWT_ALGORITHM)
                        .setBuffer(Buffer.buffer(publicKey))
                )
        )

        router.route().handler(
            CorsHandler.create("*")
                .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                .allowedMethod(io.vertx.core.http.HttpMethod.POST)
                .allowedMethod(io.vertx.core.http.HttpMethod.OPTIONS)
                .allowedHeader("Authorization")
                .allowedHeader("Access-Control-Request-Method")
                .allowedHeader("Access-Control-Allow-Credentials")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Headers")
                .allowedHeader("Content-Type")
        )

        authHandler = JWTAuthHandler.create(authProvider)

        router.route().handler(BodyHandler.create())

        registerErrorHandlers()

        logger.info { "starting cloud http api on on port $port" }
        listen = server.requestHandler(router).listen(port)
        /*
        {
            if (it.succeeded()) {
                logger.info { "cloud http api started on port ${server.actualPort()}" }
            } else {
                logger.error(it.cause()) { "starting cloud http api has failed" }
            }
        }*/
    }

    fun createSubRouter(path: String): Router {
        val subRouter = Router.router(vertx)

        subRouter.route().handler(authHandler)
        router.mountSubRouter(path, subRouter)

        return subRouter
    }

    fun configureSubRouter(path: String, configure: (Router) -> Unit) {
        val subRouter = Router.router(vertx)
        subRouter.route().handler(authHandler)
        router.mountSubRouter(path, subRouter)
        configure(subRouter)
    }

    fun configureSubRouter(path: String, configure: (Router, JWTAuthHandler) -> Unit) {
        val subRouter = Router.router(vertx)
        router.mountSubRouter(path, subRouter)
        configure(subRouter, authHandler)
    }

    fun addUnprotectedRouter(path: String): Router {
        val subRouter = Router.router(vertx)
        router.mountSubRouter(path, subRouter)
        return subRouter
    }

    private fun registerErrorHandlers() {
        server.exceptionHandler {
            logger.error(it) {
                "unhandled error"
            }
        }

        server.invalidRequestHandler {
            logger.warn {
                "invalid request '${it.path()}'"
            }
        }

        router.errorHandler(401) {
            it.jsonResponse(GenericApiResponse(listOf(MessageResponse(code = ErrorCodes.UNAUTHORIZED))), 401)
        }
    }

    fun waitForShutdown() {
        listen.succeeded()
    }
}
