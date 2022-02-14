package de.solidblocks.cloud

import java.time.Duration

object CloudConstants {

    val ENVIRONMENT_HEALTHCHECK_INTERVAL = Duration.ofSeconds(15)

    val ADMIN_USER_ENVRIONMENT_VARIABLE = "ADMIN_USER"

    val ADMIN_PASSWORD_ENVRIONMENT_VARIABLE = "ADMIN_PASSWORD"
}
