package de.solidblocks.provisioner.minio.policy

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.minio.MinioMcWrapper
import mu.KotlinLogging

class MinioPolicyProvisioner(minioCredentialsProvider: () -> MinioCredentials) :
    IResourceLookupProvider<IMinioPolicyLookup, MinioPolicyRuntime>,
    IInfrastructureResourceProvisioner<MinioPolicy, MinioPolicyRuntime> {

    private val minioMcWrapper: MinioMcWrapper

    init {
        minioMcWrapper = MinioMcWrapper(minioCredentialsProvider)
    }

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: MinioPolicy): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, missing = false)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: MinioPolicy): Result<*> {
        minioMcWrapper.addPolicy(resource.name, resource.policy)
        return Result<Any>(resource)
    }

    override fun lookup(resource: IMinioPolicyLookup): Result<MinioPolicyRuntime> {
        val user = minioMcWrapper.listPolicies().firstOrNull { it.policy == resource.name }

        return if (user != null) {
            Result.resultOf(MinioPolicyRuntime(resource.name))
        } else {
            Result.emptyResult()
        }
    }

    override val resourceType = MinioPolicy::class.java

    override val lookupType = IMinioPolicyLookup::class.java
}
