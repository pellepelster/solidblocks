package de.solidblocks.cli.commands.api

import de.solidblocks.cloud.BaseApplicationContext
import de.solidblocks.cloud.environments.EnvironmentScheduler

class ApiApplicationContext(jdbcUrl: String) : BaseApplicationContext(jdbcUrl) {

    val environmentScheduler: EnvironmentScheduler

    init {
        environmentScheduler = EnvironmentScheduler(this.database.datasource, repositories.environments, provisionerContext)
        environmentScheduler.startScheduler()
    }
}
