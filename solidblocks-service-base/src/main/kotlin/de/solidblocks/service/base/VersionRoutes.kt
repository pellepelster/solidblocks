package de.solidblocks.service.base

import de.solidblocks.base.solidblocksVersion
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

data class VersionResponse(val version: String)

fun Route.versionRoutes() {

    route("/v1/version") {
        get {
            call.respond(VersionResponse(solidblocksVersion()))
        }
    }
}
