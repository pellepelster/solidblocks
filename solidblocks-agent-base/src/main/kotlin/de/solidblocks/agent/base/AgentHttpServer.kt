package de.solidblocks.agent.base

import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import mu.KotlinLogging
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess

class AgentHttpServer(port: Int = 8080) {

    private val logger = KotlinLogging.logger {}

    val shutdown = CountDownLatch(1)

    var server: HttpServer

    init {
        val vertx = Vertx.vertx()
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())

        server = vertx.createHttpServer()
        BaseAgentRoutes(vertx, router, shutdown)

        logger.info { "starting agent http api on port $port" }
        server.requestHandler(router).listen(port)
    }

    fun waitForShutdown() {
        shutdown.await()
        server.close().result()
        exitProcess(7)
    }
}
