package de.solidblocks.cloud.provisioner.aws.iam

import de.solidblocks.cloud.api.resources.BaseInfrastructureResourceRuntime

class AwsIamUserRuntime(
    val name: String,
    val arn: String,
    val inlinePolicy: String,
) : BaseInfrastructureResourceRuntime()
