package de.solidblocks.agent.base

import de.solidblocks.agent.base.api.AGENT_BASE_PATH
import de.solidblocks.agent.base.api.TriggerShutdownRequest.TRIGGER_SHUTDOWN_PATH
import de.solidblocks.agent.base.api.TriggerUpdateRequest
import de.solidblocks.agent.base.api.TriggerUpdateRequest.Companion.TRIGGER_UPDATE_PATH
import de.solidblocks.agent.base.api.TriggerUpdateResponse
import de.solidblocks.agent.base.api.VersionResponse
import de.solidblocks.agent.base.api.VersionResponse.Companion.VERSION_PATH
import de.solidblocks.base.solidblocksVersion
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.CountDownLatch

fun Route.versionRoutes(shutdown: CountDownLatch) {

    val logger = KotlinLogging.logger {}

    route(AGENT_BASE_PATH) {

        get(VERSION_PATH) {
            call.respond(VersionResponse(solidblocksVersion()))
        }

        post(TRIGGER_SHUTDOWN_PATH) {
            call.respond(object {})
            shutdown.countDown()
        }

        post(TRIGGER_UPDATE_PATH) {
            val updateRequest = call.receive<TriggerUpdateRequest>()
            val workingDir = System.getProperty("user.dir")
            val file = File(workingDir, "update.version")

            logger.info { "writing update trigger for version '${updateRequest.updateVersion}' to '$file'" }
            file.writeText(updateRequest.updateVersion)
            call.respond(TriggerUpdateResponse(true))
            shutdown.countDown()
        }
    }
}
