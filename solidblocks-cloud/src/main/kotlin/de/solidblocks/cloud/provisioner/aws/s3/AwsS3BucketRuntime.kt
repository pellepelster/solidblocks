package de.solidblocks.cloud.provisioner.aws.s3

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class AwsS3BucketRuntime(
    val name: String,
    val region: String,
) : BaseInfrastructureResourceRuntime()
