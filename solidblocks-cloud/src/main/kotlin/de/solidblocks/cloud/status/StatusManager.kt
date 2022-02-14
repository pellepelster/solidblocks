package de.solidblocks.cloud.status

import de.solidblocks.cloud.model.repositories.EnvironmentsRepository
import de.solidblocks.cloud.model.repositories.StatusRepository
import mu.KotlinLogging
import java.time.Duration
import java.util.*

class StatusManager(val repository: StatusRepository, val environmentsRepository: EnvironmentsRepository) {

    private val logger = KotlinLogging.logger {}

    fun updateStatus(entityId: UUID, status: Status) {
        logger.info { "updating '$entityId' to $status" }
        repository.updateStatus(entityId, status.toString(), "")
    }

    fun isOk(entityId: UUID, interval: Duration): Boolean {
        return Status.OK.toString().equals(repository.latestStatus(entityId, interval))
    }
}
