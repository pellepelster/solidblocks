package de.solidblocks.cloud.services.github.model

import de.solidblocks.cloud.services.BackupRuntime
import de.solidblocks.cloud.services.ServiceCommonRuntime
import de.solidblocks.cloud.services.ServiceConfigurationRuntime

class GithubRunnerServiceConfigurationRuntime(
    override val index: Int,
    override val common: ServiceCommonRuntime,
    val labels: List<String>,
    val packages: List<String>,
    val allowSudo: Boolean,
    val scale: Int,
) : ServiceConfigurationRuntime {
    override val backup = BackupRuntime(null, 0)
}
