package de.solidblocks.provisioner.minio.policyassignment

import de.solidblocks.core.IResourceLookup
import de.solidblocks.provisioner.minio.policy.MinioPolicy
import de.solidblocks.provisioner.minio.user.MinioUser

interface IMinioPolicyAssignmentLookup : IResourceLookup<MinioPolicyAssignmentRuntime> {
    val user: MinioUser
    val policy: MinioPolicy
}
