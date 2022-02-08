package de.solidblocks.cli.commands.api

import de.solidblocks.cloud.BaseApplicationContext
import de.solidblocks.cloud.environments.EnvironmentScheduler
import de.solidblocks.cloud.tenants.TenantsScheduler
import de.solidblocks.config.db.tables.references.SCHEDULED_TASKS
import de.solidblocks.config.db.tables.references.SHEDLOCK
import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider

class ApiApplicationContext(jdbcUrl: String) : BaseApplicationContext(jdbcUrl) {

    private val environmentScheduler: EnvironmentScheduler

    private val tenantsScheduler: TenantsScheduler

    private val logger = KotlinLogging.logger {}

    init {
        val executor = DefaultLockingTaskExecutor(JdbcLockProvider(database.datasource))
        cleanupEphemeralData()

        environmentScheduler =
            EnvironmentScheduler(this.database.datasource, repositories.environments, provisionerContext, executor)
        environmentScheduler.startScheduler()

        tenantsScheduler =
            TenantsScheduler(
                this.database.datasource,
                provisionerContext,
                executor,
                managers.tenants,
                status.tenants,
                status.environments
            )
        tenantsScheduler.startScheduler()
    }

    private fun cleanupEphemeralData() {
        val dsl = database.dsl

        logger.info { "cleaning leftover locks from '${SHEDLOCK.name}'" }
        dsl.deleteFrom(SHEDLOCK).execute()

        logger.info { "cleaning leftover scheduled tasks from '${SCHEDULED_TASKS}'" }
        dsl.deleteFrom(SCHEDULED_TASKS).execute()

        repositories.status.cleanupEphemeralData()
    }
}
