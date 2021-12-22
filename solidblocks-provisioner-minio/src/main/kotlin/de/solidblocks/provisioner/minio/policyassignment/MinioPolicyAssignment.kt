package de.solidblocks.provisioner.minio.policyassignment

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.core.IResource
import de.solidblocks.provisioner.minio.policy.MinioPolicy
import de.solidblocks.provisioner.minio.user.MinioUser

class MinioPolicyAssignment(override val user: MinioUser, override val policy: MinioPolicy) :
    IMinioPolicyAssignmentLookup,
    IInfrastructureResource<MinioPolicyAssignment, MinioPolicyAssignmentRuntime> {

    override fun id() = "${user.name}-${policy.name}"

    override fun getParents(): Set<IResource> {
        return setOf(user, policy)
    }
}
