package de.solidblocks.cloud.provisioner.aws.iam

import de.solidblocks.cloud.api.resources.BaseInfrastructureResource

class AwsIamUser(
    name: String,
    val inlinePolicy: String,
) : BaseInfrastructureResource<AwsIamUserRuntime>(name, emptySet()) {

    val policyName = "$name-policy"

    fun asLookup() = AwsIamUserLookup(name)

    override fun logText() = "IAM user '$name'"

    override val lookupType = AwsIamUserLookup::class
}
