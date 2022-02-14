package de.solidblocks.cloud

import de.solidblocks.cloud.environments.EnvironmentsStatusManager
import de.solidblocks.cloud.model.repositories.RepositoriesContext
import de.solidblocks.cloud.status.StatusManager
import de.solidblocks.cloud.tenants.TenantsStatusManager

class StatusContext(val repositories: RepositoriesContext) {
    val status = StatusManager(repositories.status, repositories.environments)
    val tenants = TenantsStatusManager(status, repositories.environments)
    val environments = EnvironmentsStatusManager(status)
}
