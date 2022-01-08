package de.solidblocks.provisioner.minio.policyassignment

import de.solidblocks.api.resources.ResourceDiff
import de.solidblocks.api.resources.infrastructure.IInfrastructureResourceProvisioner
import de.solidblocks.api.resources.infrastructure.IResourceLookupProvider
import de.solidblocks.core.Result
import de.solidblocks.provisioner.minio.MinioCredentials
import de.solidblocks.provisioner.minio.MinioMcWrapper
import mu.KotlinLogging

class MinioPolicyAssignmentProvisioner(minioCredentialsProvider: () -> MinioCredentials) :
    IResourceLookupProvider<IMinioPolicyAssignmentLookup, MinioPolicyAssignmentRuntime>,
    IInfrastructureResourceProvisioner<MinioPolicyAssignment, MinioPolicyAssignmentRuntime> {

    private val minioMcWrapper: MinioMcWrapper

    init {
        minioMcWrapper = MinioMcWrapper(minioCredentialsProvider)
    }

    private val logger = KotlinLogging.logger {}

    override fun diff(resource: MinioPolicyAssignment): Result<ResourceDiff> {
        return lookup(resource).mapResourceResultOrElse(
            {
                ResourceDiff(resource, missing = false)
            },
            {
                ResourceDiff(resource, missing = true)
            }
        )
    }

    override fun apply(resource: MinioPolicyAssignment) =
        if (minioMcWrapper.assignPolicy(resource.policy.name, resource.user.name)) {
            Result<Any>(resource)
        } else {
            Result.failedResult()
        }

    override fun lookup(resource: IMinioPolicyAssignmentLookup): Result<MinioPolicyAssignmentRuntime> {
        val user = minioMcWrapper.getUser(resource.user.name) ?: return Result.emptyResult()
        val policy = minioMcWrapper.getPolicy(resource.policy.name) ?: return Result.emptyResult()

        if (user.policies.contains(policy.policy)) {
            return Result.resultOf(MinioPolicyAssignmentRuntime(policy.policy))
        }

        return Result.emptyResult()
    }

    override val resourceType = MinioPolicyAssignment::class.java

    override val lookupType = IMinioPolicyAssignmentLookup::class.java
}
