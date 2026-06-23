package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.Error
import de.solidblocks.cloud.api.Result
import de.solidblocks.cloud.api.Success
import de.solidblocks.cloud.api.resources.BaseResource
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.secret.GenericSecret
import de.solidblocks.cloud.provisioner.secret.GenericSecretRuntime
import de.solidblocks.cloud.provisioner.secret.OneTimeGeneratedSecret
import de.solidblocks.cloud.services.ServiceRegistration
import de.solidblocks.utils.log.LogContext
import java.security.KeyPair

class ProvisionerApplyContextImpl(
    sshKeyPair: KeyPair,
    sshKeyAbsolutePath: String,
    environment: EnvironmentContext,
    registry: ProvisionersRegistry,
    serviceRegistrations: List<ServiceRegistration<*, *>>,
    override val log: LogContext,
    private val taintedResources: Set<BaseResource> = emptySet(),
) : ProvisionerContextImpl(sshKeyPair, sshKeyAbsolutePath, environment, registry, serviceRegistrations), ProvisionerApplyContext {

    override fun isTainted(resource: BaseResource) = taintedResources.contains(resource)

    override suspend fun createSecret(path: String, secret: String, taintable: Boolean): Result<Unit> {
        val genericSecret = GenericSecret(
            path,
            OneTimeGeneratedSecret(secret = {
                secret
            }),
            taintable,
        )

        return when (
            val result: Result<GenericSecretRuntime> =
                registry.apply(genericSecret, this)
        ) {
            is Error<GenericSecretRuntime> -> Error(result.error)
            is Success<GenericSecretRuntime> -> Success(Unit)
        }
    }
}
