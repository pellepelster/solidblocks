package de.solidblocks.cloud.utils

import de.solidblocks.base.defaultHttpClient
import mu.KotlinLogging
import okhttp3.Request

class HttpHealthCheck {

    companion object {

        private val logger = KotlinLogging.logger {}

        fun check(url: String): Boolean {

            val client = defaultHttpClient()

            val request: Request = Request.Builder().url(url).build()

            try {
                client.newCall(request).execute().use {
                    if (it.code in 200..399) {
                        logger.info { "http healthcheck for '${url}' succeeded" }
                        return true
                    }

                    logger.info { "http healthcheck for '${url}' returned ${it.code}" }

                    return false
                }
            } catch (e: Exception) {
                logger.debug(e) { "http healthcheck for '${url}' failed" }
                return false
            }
        }
    }

}