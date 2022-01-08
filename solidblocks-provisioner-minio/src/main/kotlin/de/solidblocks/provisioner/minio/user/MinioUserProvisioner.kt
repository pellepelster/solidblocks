package de.solidblocks.provisioner.minio.user

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.minio.MinioMcWrapper
import mu.KotlinLogging

class MinioUserProvisioner(minioCredentialsProvider: () -> MinioCredentials) :
    IResourceLookupProvider<IMinioUserLookup, MinioUserRuntime>,
    IInfrastructureResourceProvisioner<MinioUser, MinioUserRuntime> {

    private val minioMcWrapper: MinioMcWrapper

    init {
        minioMcWrapper = MinioMcWrapper(minioCredentialsProvider)
    }

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: MinioUser): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, missing = false)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: MinioUser): Result<*> {
        minioMcWrapper.addUser(resource.name, resource.secretKey)
        return Result<Any>(resource)
    }

    override fun lookup(resource: IMinioUserLookup): Result<MinioUserRuntime> {
        val user = minioMcWrapper.listUsers().firstOrNull { it.accessKey == resource.name }

        return if (user != null) {
            Result.resultOf(MinioUserRuntime(resource.name, user.accessKey))
        } else {
            Result.emptyResult()
        }
    }

    override val resourceType = MinioUser::class.java

    override val lookupType = IMinioUserLookup::class.java
}
