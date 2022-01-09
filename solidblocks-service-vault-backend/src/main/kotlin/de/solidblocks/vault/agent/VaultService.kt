package de.solidblocks.vault.agent

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.solidblocks.api.resources.ResourceGroup
import de.solidblocks.base.ServiceReference
import de.solidblocks.cloud.model.ModelConstants.serviceBucketName
import de.solidblocks.cloud.model.ModelConstants.serviceConfigPath
import de.solidblocks.cloud.model.ModelConstants.serviceId
import de.solidblocks.cloud.model.ServiceRepository
import de.solidblocks.cloud.model.entities.getRawConfigValue
import de.solidblocks.provisioner.Provisioner
import de.solidblocks.provisioner.minio.Minio.MINIO_SERVICE_ACCESS_KEY_KEY
import de.solidblocks.provisioner.minio.Minio.MINIO_SERVICE_SECRET_KEY_KEY
import de.solidblocks.provisioner.minio.MinioMcWrapper
import de.solidblocks.provisioner.minio.bucket.MinioBucket
import de.solidblocks.provisioner.minio.policy.MinioPolicy
import de.solidblocks.provisioner.minio.policyassignment.MinioPolicyAssignment
import de.solidblocks.provisioner.minio.user.MinioUser
import de.solidblocks.provisioner.vault.kv.VaultKV
import de.solidblocks.provisioner.vault.mount.VaultMountLookup
import de.solidblocks.vault.VaultConstants
import java.util.*

class VaultService(val reference: ServiceReference, val serviceRepository: ServiceRepository) {

    fun createService(): Boolean {
        serviceRepository.createService(
            reference,
            mapOf(
                MINIO_SERVICE_ACCESS_KEY_KEY to serviceId(reference),
                MINIO_SERVICE_SECRET_KEY_KEY to UUID.randomUUID().toString()
            )
        )

        return true
    }

    fun getServiceConfiguration(): VaultServiceConfiguration? {
        val service = serviceRepository.getService(reference) ?: return null

        return VaultServiceConfiguration(
            service.configValues.getRawConfigValue(MINIO_SERVICE_ACCESS_KEY_KEY),
            service.configValues.getRawConfigValue(MINIO_SERVICE_SECRET_KEY_KEY)
        )
    }

    fun bootstrapService(
        provisioner: Provisioner
    ): Boolean {
        val service = getServiceConfiguration() ?: return false

        val group = ResourceGroup("${serviceId(reference)}-backup", emptySet())

        val bucket = MinioBucket(serviceBucketName(reference))
        group.addResource(bucket)

        val user = MinioUser(service.minioAccessKey, service.minioSecretKey)
        group.addResource(user)

        val policy = MinioPolicy(
            UUID.randomUUID().toString(),
            MinioMcWrapper.Policy(
                statement = listOf(
                    MinioMcWrapper.Statement(
                        action = listOf("s3:ListBucket", "s3:*", "s3:PutObject"),
                        resource = listOf(
                            "arn:aws:s3:::${serviceBucketName(reference)}/*",
                            "arn:aws:s3:::${serviceBucketName(reference)}",
                        )
                    )
                )
            )
        )
        group.addResource(policy)

        group.addResource(MinioPolicyAssignment(user, policy))

        provisioner.addResourceGroup(group)

        val mount = VaultMountLookup(VaultConstants.kvMountName(reference.toEnvironment()))

        val kv = VaultKV(
            path = serviceConfigPath(reference),
            mount = mount,
            data = jacksonObjectMapper().convertValue(service, Map::class.java) as Map<String, Any>
        )
        group.addResource(kv)

        return provisioner.apply()
    }
}
