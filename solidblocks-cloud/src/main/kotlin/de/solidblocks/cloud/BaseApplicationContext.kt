package de.solidblocks.cloud

import de.solidblocks.base.reference.EnvironmentReference
import de.solidblocks.cloud.environments.EnvironmentApplicationContext
import de.solidblocks.cloud.model.SolidblocksDatabase
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import mu.KotlinLogging

public open class BaseApplicationContext(jdbcUrl: String, startSchedulers: Boolean = true, development: Boolean = false) {

    private val logger = KotlinLogging.logger {}

    val repositories: RepositoriesContext
    val managers: ManagersContext
    val provisionerContext: ProvisionerContext
    val database: SolidblocksDatabase
    val status: StatusContext
    val schedulerContext: SchedulerContext

    init {
        database = SolidblocksDatabase(jdbcUrl)
        database.ensureDBSchema()

        repositories = RepositoriesContext(database.dsl)
        status = StatusContext(repositories)
        provisionerContext = ProvisionerContext(repositories, status)

        schedulerContext = SchedulerContext(database, repositories, status, provisionerContext)
        managers = ManagersContext(database.dsl, repositories, schedulerContext, development)
    }

    fun createEnvironmentContext(reference: EnvironmentReference) = EnvironmentApplicationContext(reference, repositories.environments)
}
