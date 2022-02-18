package de.solidblocks.cli.commands.api

import com.github.ajalt.clikt.core.CliktError
import de.solidblocks.cloud.BaseApplicationContext
import de.solidblocks.cloud.CloudConstants.ADMIN_PASSWORD_ENVRIONMENT_VARIABLE
import de.solidblocks.cloud.CloudConstants.ADMIN_USER
import mu.KotlinLogging

class ApiApplicationContext(jdbcUrl: String) : BaseApplicationContext(jdbcUrl) {

    private val logger = KotlinLogging.logger {}

    init {
        if (System.getenv(ADMIN_PASSWORD_ENVRIONMENT_VARIABLE) == null) {
            throw CliktError("admin credentials are not set, '$ADMIN_PASSWORD_ENVRIONMENT_VARIABLE' is missing")
        }
        managers.users.ensureAdminUser(
            ADMIN_USER,
            System.getenv(ADMIN_PASSWORD_ENVRIONMENT_VARIABLE)
        )
    }
}
