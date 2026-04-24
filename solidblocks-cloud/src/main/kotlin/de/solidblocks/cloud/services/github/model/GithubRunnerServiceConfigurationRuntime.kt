package de.solidblocks.cloud.services.github.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.InstanceRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

class GithubRunnerServiceConfigurationRuntime(
    override val index: Int,
    override val name: String,
    val labels: List<String>,
    override val instance: InstanceRuntime,
) : ServiceConfigurationRuntime {
    override val backup = BackupRuntime(null, 0)
}
