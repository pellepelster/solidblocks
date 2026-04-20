package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.BaseResource

interface ProvisionerDiffContext : ProvisionerContext {
    fun hasPendingChange(resource: BaseResource): Boolean
}
