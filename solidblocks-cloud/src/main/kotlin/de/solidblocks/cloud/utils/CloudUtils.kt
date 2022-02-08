package de.solidblocks.cloud.utils

import java.net.URI

object CloudUtils {

    fun extractRootDomain(host: String?): String? {
        if (host?.trim().isNullOrBlank()) {
            return null
        }

        return try {
            val uri = URI.create(host!!)
            uri.host ?: host
        } catch (e: Exception) {
            host
        }
    }
}
