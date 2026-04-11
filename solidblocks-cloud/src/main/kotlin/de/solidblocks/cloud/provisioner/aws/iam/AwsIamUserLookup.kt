package de.solidblocks.cloud.provisioner.aws.iam

import de.solidblocks.cloud.api.resources.InfrastructureResourceLookup

class AwsIamUserLookup(name: String) : InfrastructureResourceLookup<AwsIamUserRuntime>(name, emptySet()) {
    override fun logText() = "IAM user '$name'"
}
