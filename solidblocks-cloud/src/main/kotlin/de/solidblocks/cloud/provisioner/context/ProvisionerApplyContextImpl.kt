package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.OneTimeGeneratedSecret
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.cloud.utils.Error
import de.solidblocks.cloud.utils.Result
import de.solidblocks.cloud.utils.Success
import de.solidblocks.utils.LogContext
import java.security.KeyPair

class ProvisionerApplyContextImpl(
    sshKeyPair: KeyPair,
    sshKeyAbsolutePath: String,
    environment: EnvironmentContext,
    registry: ProvisionersRegistry,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
    private val taintedResources: Set<BaseResource> = emptySet(),
) : ProvisionerContextImpl(sshKeyPair, sshKeyAbsolutePath, environment, registry, serviceRegistrations), ProvisionerApplyContext {

    override fun isTainted(resource: BaseResource) = taintedResources.contains(resource)

    override suspend fun destroy(lookup: InfrastructureResourceLookup<*>, log: LogContext) = TODO("Not yet implemented")

    override suspend fun createSecret(path: String, secret: String, taintable: Boolean): Result<Unit> {
        val secret = GenericSecret<GenericSecretRuntime>(
            path,
            OneTimeGeneratedSecret(secret = {
                secret
            }),
            taintable,
        )

        return when (
            val result: Result<GenericSecretRuntime> =
                registry.apply(secret, ProvisionerApplyContextImpl(sshKeyPair, sshKeyAbsolutePath, environment, registry, serviceRegistrations), LogContext())
        ) {
            is Error<GenericSecretRuntime> -> Error(result.error)
            is Success<GenericSecretRuntime> -> Success(Unit)
        }
    }
}
