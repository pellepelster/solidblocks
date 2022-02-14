package de.solidblocks.cloud

import de.solidblocks.cloud.environments.EnvironmentScheduler
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.cloud.tenants.TenantsScheduler
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider

class SchedulerContext(
    val database: SolidblocksDatabase,
    val repositories: RepositoriesContext,
    val status: StatusContext,
    val provisionerContext: ProvisionerContext,
    startSchedulers: Boolean = true
) {

    val environments: EnvironmentScheduler

    val tenants: TenantsScheduler

    init {

        val executor = DefaultLockingTaskExecutor(JdbcLockProvider(database.datasource))

        environments =
            EnvironmentScheduler(database.datasource, repositories.environments, provisionerContext, executor)

        tenants = TenantsScheduler(
            this.database.datasource,
            provisionerContext,
            executor,
            repositories.tenants,
            status.tenants,
            status.environments
        )

        if (startSchedulers) {
            environments.startScheduler()
            tenants.startScheduler()
        }
    }
}
