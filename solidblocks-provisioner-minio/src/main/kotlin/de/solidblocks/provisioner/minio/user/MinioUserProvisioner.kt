package de.solidblocks.provisioner.minio.user

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.minio.MinioMcWrapper
import mu.KotlinLogging
import java.util.*

class MinioUserProvisioner(minioCredentialsProvider: () -> MinioCredentials) :
    IResourceLookupProvider<IMinioUserLookup, MinioUserRuntime>,
    IInfrastructureResourceProvisioner<MinioUser, MinioUserRuntime> {

    private val minioMcWrapper: MinioMcWrapper

    init {
        minioMcWrapper = MinioMcWrapper(minioCredentialsProvider)
    }

    private val logger = KotlinLogging.logger {}

    override fun getResourceType(): Class<MinioUser> {
        return MinioUser::class.java
    }

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
        minioMcWrapper.addUser(resource.name, UUID.randomUUID().toString())
        return Result<Any>(resource)
    }

    override fun lookup(resource: IMinioUserLookup): Result<MinioUserRuntime> {
        val user = minioMcWrapper.listUsers().firstOrNull { it.accessKey == resource.name() }

        return if (user != null) {
            Result.resultOf(MinioUserRuntime(resource.name(), user.accessKey))
        } else {
            Result.emptyResult()
        }
    }

    override fun getLookupType(): Class<*> {
        return IMinioUserLookup::class.java
    }
}
