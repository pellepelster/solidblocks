package de.solidblocks.provisioner.minio

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cloud.config.model.CloudEnvironmentConfiguration
import de.solidblocks.provisioner.minio.bucket.MinioBucketProvisioner

object Minio {

    // TODO use https and cloud pki for connections
    fun minioAddress(environment: CloudEnvironmentConfiguration) =
        "http://backup.${environment.name}.${environment.cloud.rootDomain}:9000"

    fun registerProvisioners(
        provisionerRegistry: ProvisionerRegistry,
        minioCredentialsProvider: () -> MinioCredentials
    ) {
        provisionerRegistry.addProvisioner(
            MinioBucketProvisioner(minioCredentialsProvider) as IInfrastructureResourceProvisioner<Any, Any>
        )
    }
}
