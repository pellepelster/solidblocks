package de.solidblocks.provisioner.minio

import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.base.ProvisionerRegistry
import de.solidblocks.cloud.model.entities.EnvironmentEntity
import de.solidblocks.provisioner.minio.bucket.MinioBucketProvisioner
import de.solidblocks.provisioner.minio.policy.MinioPolicyProvisioner
import de.solidblocks.provisioner.minio.policyassignment.MinioPolicyAssignmentProvisioner
import de.solidblocks.provisioner.minio.user.MinioUserProvisioner

object Minio {

    const val MINIO_SERVICE_ACCESS_KEY_KEY = "minio_service_access_key"
    const val MINIO_SERVICE_SECRET_KEY_KEY = "minio_service_secret_key"

    // TODO use https and cloud pki for connections
    fun minioAddress(environment: EnvironmentEntity) =
        "http://backup.${environment.name}.${environment.cloud.rootDomain}:9000"

    fun registerProvisioners(
        provisionerRegistry: ProvisionerRegistry,
        minioCredentialsProvider: () -> MinioCredentials
    ) {
        provisionerRegistry.addProvisioner(
            MinioBucketProvisioner(minioCredentialsProvider) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            MinioPolicyProvisioner(minioCredentialsProvider) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            MinioPolicyAssignmentProvisioner(minioCredentialsProvider) as IInfrastructureResourceProvisioner<Any, Any>
        )

        provisionerRegistry.addProvisioner(
            MinioUserProvisioner(minioCredentialsProvider) as IInfrastructureResourceProvisioner<Any, Any>
        )
    }
}
