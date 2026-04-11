package de.solidblocks.cloud.provisioner.aws.s3

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class AwsS3Bucket(
    name: String,
    val region: String,
) : BaseInfrastructureResource<AwsS3BucketRuntime>(name, emptySet()) {

    fun asLookup() = AwsS3BucketLookup(name)

    override fun logText() = "S3 bucket '$name'"

    override val lookupType = AwsS3BucketLookup::class
}
