package de.solidblocks.cloud.provisioner.aws.s3

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class AwsS3BucketLookup(name: String) : InfrastructureResourceLookup<AwsS3BucketRuntime>(name, emptySet()) {
    override fun logText() = "S3 bucket '$name'"
}
