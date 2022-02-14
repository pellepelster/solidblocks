package de.solidblocks.cli.commands.api

import com.github.ajalt.clikt.core.CliktError
import de.solidblocks.cloud.BaseApplicationContext
import de.solidblocks.cloud.CloudConstants.ADMIN_PASSWORD_ENVRIONMENT_VARIABLE
import de.solidblocks.cloud.CloudConstants.ADMIN_USER_ENVRIONMENT_VARIABLE
import de.solidblocks.config.db.tables.references.SCHEDULED_TASKS
import de.solidblocks.config.db.tables.references.SHEDLOCK
import mu.KotlinLogging

class ApiApplicationContext(jdbcUrl: String) : BaseApplicationContext(jdbcUrl) {

    private val logger = KotlinLogging.logger {}

    init {
        if (System.getenv(ADMIN_USER_ENVRIONMENT_VARIABLE) == null || System.getenv(ADMIN_PASSWORD_ENVRIONMENT_VARIABLE) == null) {
            throw CliktError("admin credentials are not set, either '$ADMIN_USER_ENVRIONMENT_VARIABLE' or '$ADMIN_PASSWORD_ENVRIONMENT_VARIABLE' is missing")
        }
        managers.users.ensureAdminUser(
            System.getenv(ADMIN_USER_ENVRIONMENT_VARIABLE),
            System.getenv(ADMIN_PASSWORD_ENVRIONMENT_VARIABLE)
        )

        cleanupEphemeralData()
    }

    private fun cleanupEphemeralData() {
        val dsl = database.dsl

        logger.info { "cleaning leftover locks from '${SHEDLOCK.name}'" }
        dsl.deleteFrom(SHEDLOCK).execute()

        logger.info { "cleaning leftover scheduled tasks from '$SCHEDULED_TASKS'" }
        dsl.deleteFrom(SCHEDULED_TASKS).execute()

        repositories.status.cleanupEphemeralData()
    }
}
