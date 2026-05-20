package de.solidblocks.cloud.provisioner.context

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup
import de.solidblocks.cloud.configuration.model.EnvironmentContext
import de.solidblocks.cloud.provisioner.ProvisionersRegistry
import de.solidblocks.cloud.provisioner.pass.OneTimeGeneratedSecret
import de.solidblocks.cloud.provisioner.pass.PassSecret
import de.solidblocks.cloud.provisioner.pass.PassSecretRuntime
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
) : ProvisionerContextImpl(sshKeyPair, sshKeyAbsolutePath, environment, registry, serviceRegistrations), ProvisionerApplyContext {

    override suspend fun destroy(lookup: InfrastructureResourceLookup<*>, log: LogContext) = TODO("Not yet implemented")

    override suspend fun createSecret(path: String, secret: String, taintable: Boolean): Result<Unit> {
        val secret = PassSecret(
            path,
            OneTimeGeneratedSecret(secret = {
                secret
            }),
            taintable,
        )

        return when (
            val result: Result<PassSecretRuntime> =
                registry.apply(secret, ProvisionerApplyContextImpl(sshKeyPair, sshKeyAbsolutePath, environment, registry, serviceRegistrations), LogContext())
        ) {
            is de.solidblocks.cloud.utils.Error<PassSecretRuntime> -> Error(result.error)
            is Success<PassSecretRuntime> -> Success(Unit)
        }
    }
}
