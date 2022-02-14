package de.solidblocks.cloud.environments

import de.solidblocks.cloud.CloudConstants
import de.solidblocks.cloud.status.StatusManager
import mu.KotlinLogging
import java.util.*

class EnvironmentsStatusManager(
    private val statusManager: StatusManager,
) {
    private val logger = KotlinLogging.logger {}

    fun isOk(environment: UUID) = statusManager.isOk(environment, CloudConstants.ENVIRONMENT_HEALTHCHECK_INTERVAL)
}
