package de.solidblocks.provisioner.minio.policyassignment

import de.solidblocks.core.IInfrastructureResource
import de.solidblocks.provisioner.minio.policy.MinioPolicy
import de.solidblocks.provisioner.minio.user.MinioUser

class MinioPolicyAssignment(override val user: MinioUser, override val policy: MinioPolicy) :
    IMinioPolicyAssignmentLookup,
    IInfrastructureResource<MinioPolicyAssignment, MinioPolicyAssignmentRuntime> {

    override val name = "${user.name}-${policy.name}"

    override val parents = setOf(user, policy)
}
