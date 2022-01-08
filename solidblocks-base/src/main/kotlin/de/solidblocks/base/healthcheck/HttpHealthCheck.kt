package de.solidblocks.base.healthcheck

import mu.KotlinLogging
import java.net.URL

class HttpHealthCheck(private val url: String) {

    private val logger = KotlinLogging.logger {}

    fun check() = try {
        URL(url).readText()
        logger.info { "url '$url' is healthy" }
        true
    } catch (e: Exception) {
        logger.warn { "url '$url' is unhealthy" }
        false
    }
}
