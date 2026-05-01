package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.services.ServiceRegistration
import java.security.KeyPair

class ProvisionerDiffContextImpl(
    sshKeyPair: KeyPair,
    sshKeyAbsolutePath: String,
    val pendingChanges: List<BaseInfrastructureResource<*>>,
    environment: EnvironmentContext,
    registry: ProvisionersRegistry,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
) : ProvisionerContextImpl(sshKeyPair, sshKeyAbsolutePath, environment, registry, serviceRegistrations), ProvisionerDiffContext {

    override fun hasPendingChange(resource: BaseResource) = when (resource) {
        is InfrastructureResourceLookup<*> -> pendingChanges.any { pendingChanges.any { resource.isLookupFor(it) } }
        else -> pendingChanges.contains(resource)
    }

}
