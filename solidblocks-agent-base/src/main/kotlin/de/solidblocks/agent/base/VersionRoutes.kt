package de.solidblocks.agent.base

import de.solidblocks.base.solidblocksVersion
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.CountDownLatch

data class VersionResponse(val version: String)
data class TriggerUpdateRequest(val updateVersion: String)
data class TriggerUpdateResponse(val triggered: Boolean)

fun Route.versionRoutes(shutdown: CountDownLatch) {

    val logger = KotlinLogging.logger {}

    route("/v1/agent") {

        get("version") {
            call.respond(VersionResponse(solidblocksVersion()))
        }

        post("trigger-update") {
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
